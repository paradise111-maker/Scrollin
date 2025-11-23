package com.example.scrollin

import android.content.Context
import android.content.SharedPreferences

class PointsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ScrollinPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_POINTS = "total_points"
        private const val KEY_EARNED_MINUTES = "earned_minutes"
        private const val KEY_USED_MINUTES = "used_minutes"
        private const val KEY_LAST_RESET = "last_reset"
    }

    // Get current points
    fun getPoints(): Int {
        return prefs.getInt(KEY_POINTS, 0)
    }

    // Add points
    fun addPoints(points: Int) {
        val currentPoints = getPoints()
        prefs.edit().putInt(KEY_POINTS, currentPoints + points).apply()
    }

    // Get earned minutes (available to use)
    fun getEarnedMinutes(): Int {
        return prefs.getInt(KEY_EARNED_MINUTES, 0)
    }

    // Add earned minutes
    fun addEarnedMinutes(minutes: Int) {
        val currentMinutes = getEarnedMinutes()
        prefs.edit().putInt(KEY_EARNED_MINUTES, currentMinutes + minutes).apply()
    }

    // Use minutes (when user accesses blocked apps)
    fun useMinutes(minutes: Int): Boolean {
        val available = getEarnedMinutes()
        if (available >= minutes) {
            prefs.edit().putInt(KEY_EARNED_MINUTES, available - minutes).apply()

            val usedTotal = prefs.getInt(KEY_USED_MINUTES, 0)
            prefs.edit().putInt(KEY_USED_MINUTES, usedTotal + minutes).apply()
            return true
        }
        return false
    }

    // Check if user has enough minutes
    fun hasMinutesAvailable(minutes: Int): Boolean {
        return getEarnedMinutes() >= minutes
    }

    // Get total used minutes (for statistics)
    fun getUsedMinutes(): Int {
        return prefs.getInt(KEY_USED_MINUTES, 0)
    }

    // Reset daily stats (call this at midnight)
    fun resetDaily() {
        val currentTime = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_EARNED_MINUTES, 0)
            .putInt(KEY_USED_MINUTES, 0)
            .putLong(KEY_LAST_RESET, currentTime)
            .apply()
    }

    // Check if we need to reset (call this on app start)
    fun checkAndResetIfNeeded() {
        val lastReset = prefs.getLong(KEY_LAST_RESET, 0)
        val currentTime = System.currentTimeMillis()

        // Check if it's a new day (simplified - checks if 24 hours passed)
        val hoursSinceReset = (currentTime - lastReset) / (1000 * 60 * 60)
        if (hoursSinceReset >= 24) {
            resetDaily()
        }
    }

    // Get statistics
    fun getStatistics(): Map<String, Int> {
        return mapOf(
            "total_points" to getPoints(),
            "earned_today" to getEarnedMinutes(),
            "used_today" to getUsedMinutes()
        )
    }
}