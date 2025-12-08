package com.example.scrollin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "scrollin_channel"
        private const val STREAK_REMINDER_ID = 1001
        private const val MORNING_MOTIVATION_ID = 1002
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scrollin Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to keep your streak and stay motivated."
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendStreakReminder(streak: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_journey)
            .setContentTitle("🔥 Keep your streak alive!")
            .setContentText("Complete an activity to maintain your ${streak}-day streak")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
            
        notificationManager.notify(STREAK_REMINDER_ID, notification)
    }
    
    fun sendMorningMotivation() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_journey)
            .setContentTitle("☀️ Good morning!")
            .setContentText("Start your day with a productive activity")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
            
        notificationManager.notify(MORNING_MOTIVATION_ID, notification)
    }
}