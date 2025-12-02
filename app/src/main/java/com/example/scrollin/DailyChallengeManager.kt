package com.example.scrollin

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class DailyChallengeManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ScrollinPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_DAILY_CHALLENGE = "daily_challenge"
        private const val KEY_DAILY_CHALLENGE_DATE = "daily_challenge_date"
    }

    private val allChallenges = listOf(
        DailyChallenge("complete_3_activities", "Complete 3 different activities today", 50, target = 3),
        DailyChallenge("maintain_5_day_streak", "Maintain a 5-day streak", 100, target = 5),
        DailyChallenge("earn_100_points", "Earn 100 points today", 75, target = 100),
        DailyChallenge("meditate_10_minutes", "Meditate for 10 minutes", 30, target = 10)
    )

    fun getTodaysChallenge(): DailyChallenge {
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_DAILY_CHALLENGE_DATE, null)

        if (today == savedDate) {
            val json = prefs.getString(KEY_DAILY_CHALLENGE, null)
            if (json != null) {
                return gson.fromJson(json, DailyChallenge::class.java)
            }
        }

        // If no challenge for today, create a new one
        val newChallenge = allChallenges.random()
        saveChallenge(newChallenge)
        return newChallenge
    }

    fun updateChallengeProgress(progress: Int) {
        val challenge = getTodaysChallenge()
        challenge.progress = progress
        if (challenge.progress >= challenge.target) {
            challenge.isCompleted = true
        }
        saveChallenge(challenge)
    }

    fun claimChallengeReward(): Int {
        val challenge = getTodaysChallenge()
        if (challenge.isCompleted && !challenge.isClaimed) {
            challenge.isClaimed = true
            saveChallenge(challenge)
            val pointsManager = PointsManager(context)
            pointsManager.addPoints(challenge.points, Category.GENERAL) // Or a specific category for challenges
            return challenge.points
        }
        return 0
    }

    private fun saveChallenge(challenge: DailyChallenge) {
        val json = gson.toJson(challenge)
        prefs.edit()
            .putString(KEY_DAILY_CHALLENGE, json)
            .putString(KEY_DAILY_CHALLENGE_DATE, getTodayDateString())
            .apply()
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
