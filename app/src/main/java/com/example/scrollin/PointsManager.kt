package com.example.scrollin

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class PointsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ScrollinPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOTAL_POINTS = "total_points"
        private const val KEY_WEEKLY_POINTS = "weekly_points"
        private const val KEY_LAST_RESET = "last_reset"
        private const val KEY_WEEK_START = "week_start"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LAST_ACTIVITY_DATE = "last_activity_date"
        private const val KEY_WEEKEND_MINUTES_USED = "weekend_minutes_used"
        
        // NEW: Daily points tracking
        private const val KEY_DAILY_POINTS_PREFIX = "daily_points_" // e.g., "daily_points_2024-11-23"
    }

    // === DAILY POINTS TRACKING (NEW) ===
    
    fun addPoints(points: Int) {
        // Add to total (lifetime)
        val currentTotal = getTotalPoints()
        prefs.edit().putInt(KEY_TOTAL_POINTS, currentTotal + points).apply()
        
        // Add to weekly accumulation
        val currentWeekly = getWeeklyPoints()
        prefs.edit().putInt(KEY_WEEKLY_POINTS, currentWeekly + points).apply()
        
        // NEW: Add to today's points
        val today = getTodayDateString()
        val todayKey = KEY_DAILY_POINTS_PREFIX + today
        val todayPoints = prefs.getInt(todayKey, 0)
        prefs.edit().putInt(todayKey, todayPoints + points).apply()
        
        // Update streak
        updateStreak()
    }
    
    // NEW: Get points for last 7 days
    fun getLast7DaysPoints(): List<Int> {
        val pointsList = mutableListOf<Int>()
        val calendar = Calendar.getInstance()
        
        // Start from Monday of current week
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        
        // Get points for each day of the week
        for (i in 0..6) { // Monday to Sunday
            val dateString = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
            val dayKey = KEY_DAILY_POINTS_PREFIX + dateString
            val dayPoints = prefs.getInt(dayKey, 0)
            pointsList.add(dayPoints)
            
            // Move to next day
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return pointsList
    }
    
    fun getTotalPoints(): Int {
        return prefs.getInt(KEY_TOTAL_POINTS, 0)
    }
    
    fun getWeeklyPoints(): Int {
        checkAndResetWeekly()
        return prefs.getInt(KEY_WEEKLY_POINTS, 0)
    }

    // === WEEKEND SYSTEM ===
    
    fun getAvailableWeekendMinutes(): Int {
        // if (!isWeekend()) return 0 // Temporarily commented out for testing

        val weeklyPoints = getWeeklyPoints()
        val baseMinutes = weeklyPoints * 2  // 1 point = 2 minutes on weekend
        val streakBonus = getStreakBonus()
        val maxMinutes = 240  // Cap at 4 hours
        
        val totalAvailable = (baseMinutes + streakBonus).coerceAtMost(maxMinutes)
        val alreadyUsed = getWeekendMinutesUsed()
        
        return (totalAvailable - alreadyUsed).coerceAtLeast(0)
    }
    
    fun useWeekendMinutes(minutes: Int): Boolean {
        if (!isWeekend()) return false
        
        val available = getAvailableWeekendMinutes()
        if (available < minutes) return false
        
        val used = getWeekendMinutesUsed()
        prefs.edit().putInt(KEY_WEEKEND_MINUTES_USED, used + minutes).apply()
        return true
    }
    
    private fun getWeekendMinutesUsed(): Int {
        return prefs.getInt(KEY_WEEKEND_MINUTES_USED, 0)
    }
    
    fun isWeekend(): Boolean {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }
    
    // === STREAK SYSTEM ===
    
    private fun updateStreak() {
        val today = getTodayDateString()
        val lastActivity = prefs.getString(KEY_LAST_ACTIVITY_DATE, "") ?: ""
        
        if (lastActivity == today) {
            return
        }
        
        val yesterday = getYesterdayDateString()
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        
        val newStreak = if (lastActivity == yesterday) {
            currentStreak + 1
        } else if (lastActivity.isEmpty()) {
            1
        } else {
            1
        }
        
        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, newStreak)
            .putString(KEY_LAST_ACTIVITY_DATE, today)
            .apply()
    }
    
    fun getCurrentStreak(): Int {
        return prefs.getInt(KEY_CURRENT_STREAK, 0)
    }
    
    private fun getStreakBonus(): Int {
        val streak = getCurrentStreak()
        return when {
            streak >= 30 -> 120
            streak >= 14 -> 60
            streak >= 7 -> 30
            else -> 0
        }
    }
    
    // === WEEKLY RESET ===
    
    private fun checkAndResetWeekly() {
        val lastWeekStart = prefs.getLong(KEY_WEEK_START, 0)
        val currentWeekStart = getStartOfWeekTimestamp()
        
        if (currentWeekStart > lastWeekStart) {
            prefs.edit()
                .putInt(KEY_WEEKLY_POINTS, 0)
                .putInt(KEY_WEEKEND_MINUTES_USED, 0)
                .putLong(KEY_WEEK_START, currentWeekStart)
                .apply()
        }
    }
    
    // === HELPER FUNCTIONS ===
    
    private fun getTodayDateString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }
    
    private fun getYesterdayDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }
    
    private fun getStartOfWeekTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }
    
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "total_points" to getTotalPoints(),
            "weekly_points" to getWeeklyPoints(),
            "current_streak" to getCurrentStreak(),
            "weekend_minutes" to getAvailableWeekendMinutes(),
            "weekend_used" to getWeekendMinutesUsed()
        )
    }

    fun resetDaily() {
        prefs.edit().apply {
            putInt(KEY_WEEKLY_POINTS, 0)
            putInt(KEY_WEEKEND_MINUTES_USED, 0)
            putInt(KEY_CURRENT_STREAK, 0)
            // Also clear daily points for the last 7 days for a full reset
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            for (i in 0..6) {
                val dateString = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
                val dayKey = KEY_DAILY_POINTS_PREFIX + dateString
                remove(dayKey)
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            apply()
        }
    }
}