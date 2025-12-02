package com.example.scrollin

import android.content.Context
import android.content.SharedPreferences

class UserLevelManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ScrollinPrefs", Context.MODE_PRIVATE)

    private val levels = listOf(
        UserLevel(1, "Novice", 0, 100, listOf("Basic activities unlocked")),
        UserLevel(2, "Warrior", 101, 300, listOf("Yoga unlocked", "Reading unlocked")),
        UserLevel(3, "Champion", 301, 600, listOf("Advanced meditation", "Journaling unlocked")),
        UserLevel(4, "Master", 601, 1000, listOf("Custom rewards", "All activities unlocked")),
        UserLevel(5, "Legend", 1001, Int.MAX_VALUE, listOf("Exclusive badges", "Premium themes"))
    )

    fun getCurrentLevel(): UserLevel {
        val totalPoints = prefs.getInt("total_points", 0)
        return levels.findLast { totalPoints >= it.minPoints } ?: levels.first()
    }

    fun getLevelProgress(): Int {
        val currentLevel = getCurrentLevel()
        val totalPoints = prefs.getInt("total_points", 0)
        if (currentLevel.maxPoints == Int.MAX_VALUE) return 100
        val pointsIntoLevel = totalPoints - currentLevel.minPoints
        val levelRange = currentLevel.maxPoints - currentLevel.minPoints
        return ((pointsIntoLevel.toFloat() / levelRange) * 100).toInt()
    }

    fun addPoints(points: Int) {
        val currentTotal = prefs.getInt("total_points", 0)
        prefs.edit().putInt("total_points", currentTotal + points).apply()
    }
}
