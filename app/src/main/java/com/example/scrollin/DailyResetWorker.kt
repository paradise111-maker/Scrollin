package com.example.scrollin

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.Calendar

class DailyResetWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("ScrollinPrefs", Context.MODE_PRIVATE)
        
        // Reset daily counters
        prefs.edit()
            .putInt("weekend_minutes_used_today", 0)
            .putLong("weekend_session_start", 0)
            .apply()
        
        // Check and update streak
        val pointsManager = PointsManager(applicationContext)
        val lastActivityDate = prefs.getString("last_activity_date", "") ?: ""
        val yesterday = getYesterdayDateString()
        val currentStreak = prefs.getInt("current_streak", 0)
        
        // If user didn't complete any activity yesterday, break streak
        if (lastActivityDate != yesterday && currentStreak > 0) {
            prefs.edit()
                .putInt("current_streak", 0)
                .apply()
        }
        
        return Result.success()
    }

    private fun getYesterdayDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(calendar.time)
    }

    companion object {
        private const val WORK_NAME = "daily_reset_work"

        fun schedule(context: Context) {
            // Calculate delay until midnight
            val currentDate = Calendar.getInstance()
            val dueDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, 1)
            }

            val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

            // Create periodic work request (runs daily at midnight)
            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyResetWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWorkRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}