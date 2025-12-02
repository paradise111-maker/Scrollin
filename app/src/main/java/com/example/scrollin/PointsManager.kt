package com.example.scrollin

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class PointsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ScrollinPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_TOTAL_POINTS = "total_points"
        private const val KEY_WEEKLY_POINTS = "weekly_points"
        private const val KEY_WEEK_START = "week_start"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LAST_ACTIVITY_DATE = "last_activity_date"
        private const val KEY_WEEKEND_MINUTES_USED = "weekend_minutes_used"
        private const val KEY_DAILY_POINTS_PREFIX = "daily_points_"
        private const val KEY_UNLOCKED_PERKS_PREFIX = "unlocked_perk_"
        private const val KEY_UNLOCKED_BADGES_PREFIX = "unlocked_badge_"
        private const val KEY_GOALS = "goals"
        private const val KEY_TOTAL_MEDITATION_MINUTES = "total_meditation_minutes"
        private const val KEY_TOTAL_EXERCISES_COMPLETED = "total_exercises_completed"
    }

    // === POINTS & DATA TRACKING ===

    fun addPoints(points: Int, activityType: String? = null, duration: Int? = null) {
        val currentTotal = getTotalPoints()
        prefs.edit().putInt(KEY_TOTAL_POINTS, currentTotal + points).apply()

        val currentWeekly = getWeeklyPoints()
        prefs.edit().putInt(KEY_WEEKLY_POINTS, currentWeekly + points).apply()

        val today = getTodayDateString()
        val todayKey = KEY_DAILY_POINTS_PREFIX + today
        val todayPoints = prefs.getInt(todayKey, 0)
        prefs.edit().putInt(todayKey, todayPoints + points).apply()

        if (activityType == "MEDITATION" && duration != null) {
            val currentMeditationMinutes = getTotalMeditationMinutes()
            prefs.edit().putInt(KEY_TOTAL_MEDITATION_MINUTES, currentMeditationMinutes + duration).apply()
        }

        if (activityType == "PUSHUPS" || activityType == "SQUATS" || activityType == "JUMPING_JACKS") {
            val currentExercises = getTotalExercisesCompleted()
            prefs.edit().putInt(KEY_TOTAL_EXERCISES_COMPLETED, currentExercises + 1).apply()
        }

        updateStreak()
        checkAndUnlockBadges(activityType)
    }

    private fun subtractPoints(points: Int) {
        val currentTotal = getTotalPoints()
        prefs.edit().putInt(KEY_TOTAL_POINTS, (currentTotal - points).coerceAtLeast(0)).apply()

        val currentWeekly = getWeeklyPoints()
        prefs.edit().putInt(KEY_WEEKLY_POINTS, (currentWeekly - points).coerceAtLeast(0)).apply()

        val today = getTodayDateString()
        val todayKey = KEY_DAILY_POINTS_PREFIX + today
        val todayPoints = prefs.getInt(todayKey, 0)
        prefs.edit().putInt(todayKey, (todayPoints - points).coerceAtLeast(0)).apply()
    }

    fun getLast7DaysPoints(): List<Int> {
        val pointsList = mutableListOf<Int>()
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        for (i in 0..6) {
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val dayKey = KEY_DAILY_POINTS_PREFIX + dateString
            pointsList.add(prefs.getInt(dayKey, 0))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return pointsList
    }

    // === BADGE UNLOCKING LOGIC ===

    private fun checkAndUnlockBadges(activityType: String?) {
        val allBadges = getAllBadges()
        val unlockedBadges = getUnlockedBadges()

        for (badge in allBadges) {
            if (!unlockedBadges.any { it.name == badge.name }) {
                if (checkBadgeCondition(badge, activityType)) {
                    unlockBadge(badge)
                }
            }
        }
    }

    private fun checkBadgeCondition(badge: JourneyBadge, activityType: String?): Boolean {
        return when (badge.name) {
            "First Steps" -> true // Unlocked on first activity
            "Hot Streak" -> getCurrentStreak() >= 3
            "7-Day Streak" -> getCurrentStreak() >= 7
            "Taskmaster" -> getTotalGoalsCompleted() >= 10
            "Goal-Getter" -> getTotalGoalsCompleted() >= 50
            "Zen Novice" -> getTotalMeditationMinutes() >= 60
            "Zen Master" -> getTotalMeditationMinutes() >= 600
            "Fitness Fiend" -> getTotalExercisesCompleted() >= 25
            "Workout Warrior" -> getTotalExercisesCompleted() >= 100
            else -> false
        }
    }

    private fun unlockBadge(badge: JourneyBadge) {
        prefs.edit().putBoolean(KEY_UNLOCKED_BADGES_PREFIX + badge.name, true).apply()
    }

    fun getUnlockedBadges(): List<JourneyBadge> {
        val allBadges = getAllBadges()
        return allBadges.filter { prefs.getBoolean(KEY_UNLOCKED_BADGES_PREFIX + it.name, false) }
    }

    fun getAllBadges(): List<JourneyBadge> {
        return listOf(
            JourneyBadge("First Steps", "Completed 1st activity", R.drawable.ic_journey, false),
            JourneyBadge("Hot Streak", "3-day streak", R.drawable.ic_journey, false),
            JourneyBadge("7-Day Streak", "7-day streak", R.drawable.ic_journey, false),
            JourneyBadge("Taskmaster", "Completed 10 tasks", R.drawable.ic_add, false),
            JourneyBadge("Goal-Getter", "Completed 50 tasks", R.drawable.ic_add, false),
            JourneyBadge("Zen Novice", "Meditated for 60 mins", R.drawable.ic_activities, false),
            JourneyBadge("Zen Master", "Meditated for 600 mins", R.drawable.ic_activities, false),
            JourneyBadge("Fitness Fiend", "Completed 25 exercises", R.drawable.ic_profile, false),
            JourneyBadge("Workout Warrior", "Completed 100 exercises", R.drawable.ic_profile, false)
        )
    }

    private fun getTotalGoalsCompleted(): Int {
        return getGoals().count { it.isCompleted }
    }

    private fun getTotalMeditationMinutes(): Int {
        return prefs.getInt(KEY_TOTAL_MEDITATION_MINUTES, 0)
    }

    private fun getTotalExercisesCompleted(): Int {
        return prefs.getInt(KEY_TOTAL_EXERCISES_COMPLETED, 0)
    }

    // === GOALS ===

    fun addGoal(goal: JourneyGoal) {
        val goals = getGoals().toMutableList()
        goals.add(0, goal)
        saveGoals(goals)
    }

    fun getGoals(): List<JourneyGoal> {
        val json = prefs.getString(KEY_GOALS, null)
        val savedGoals: List<JourneyGoal> = if (json != null) {
            val type = object : TypeToken<List<JourneyGoal>>() {}.type
            gson.fromJson<List<JourneyGoal>>(json, type) ?: emptyList()
        } else {
            emptyList()
        }

        val defaultGoals = getBaseGoals().filter { defaultGoal ->
            savedGoals.none { it.id == defaultGoal.id }
        }

        return savedGoals + defaultGoals
    }

    private fun getBaseGoals(): List<JourneyGoal> {
        return listOf(
            JourneyGoal("morning_meditation", "Meditate for 5 minutes", 10, false, GoalType.MORNING),
            JourneyGoal("morning_exercise", "Quick 10-min workout", 15, false, GoalType.MORNING),
            JourneyGoal("night_reflect", "Journal for 5 minutes", 10, false, GoalType.NIGHT),
            JourneyGoal("night_read", "Read for 15 minutes", 15, false, GoalType.NIGHT)
        )
    }

    fun updateGoal(updatedGoal: JourneyGoal) {
        val goals = getGoals().toMutableList()
        val index = goals.indexOfFirst { it.id == updatedGoal.id }
        if (index != -1) {
            val oldGoal = goals[index]
            goals[index] = updatedGoal

            if (oldGoal.isCompleted != updatedGoal.isCompleted) {
                if (updatedGoal.isCompleted) {
                    addPoints(updatedGoal.points)
                } else {
                    subtractPoints(updatedGoal.points)
                }
            }

            saveGoals(goals)
        }
    }

    private fun saveGoals(goals: List<JourneyGoal>) {
        val json = gson.toJson(goals)
        prefs.edit().putString(KEY_GOALS, json).apply()
    }


    // === PERKS & SPENDING ===

    fun spendPoints(cost: Int): Boolean {
        val currentPoints = getTotalPoints()
        if (currentPoints < cost) {
            return false
        }
        prefs.edit().putInt(KEY_TOTAL_POINTS, currentPoints - cost).apply()
        return true
    }

    fun unlockPerk(perkId: String) {
        prefs.edit().putBoolean(KEY_UNLOCKED_PERKS_PREFIX + perkId, true).apply()
    }

    fun isPerkUnlocked(perkId: String): Boolean {
        return prefs.getBoolean(KEY_UNLOCKED_PERKS_PREFIX + perkId, false)
    }

    // === GETTERS ===

    fun getTotalPoints(): Int {
        return prefs.getInt(KEY_TOTAL_POINTS, 0)
    }

    fun getWeeklyPoints(): Int {
        checkAndResetWeekly()
        return prefs.getInt(KEY_WEEKLY_POINTS, 0)
    }

    fun getAvailableWeekendMinutes(): Int {
        val weeklyPoints = getWeeklyPoints()
        val baseMinutes = weeklyPoints * 2
        val streakBonus = getStreakBonus()
        val maxMinutes = 240
        val totalAvailable = (baseMinutes + streakBonus).coerceAtMost(maxMinutes)
        val alreadyUsed = getWeekendMinutesUsed()
        return (totalAvailable - alreadyUsed).coerceAtLeast(0)
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
        if (lastActivity == today) return

        val yesterday = getYesterdayDateString()
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val newStreak = if (lastActivity == yesterday) currentStreak + 1 else 1

        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, newStreak)
            .putString(KEY_LAST_ACTIVITY_DATE, today)
            .apply()
    }

    fun getCurrentStreak(): Int {
        return prefs.getInt(KEY_CURRENT_STREAK, 0)
    }

    private fun getStreakBonus(): Int {
        return when (getCurrentStreak()) {
            in 7..13 -> 30
            in 14..29 -> 60
            in 30..Int.MAX_VALUE -> 120
            else -> 0
        }
    }

    // === WEEKLY RESET & HELPERS ===

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

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getYesterdayDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    private fun getStartOfWeekTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }

    fun getStatistics(): Map<String, Any> {
        val totalMinutes = (getTotalPoints() / 10) * 5 // Example logic
        return mapOf(
            "total_points" to getTotalPoints(),
            "weekly_points" to getWeeklyPoints(),
            "current_streak" to getCurrentStreak(),
            "weekend_minutes" to getAvailableWeekendMinutes(),
            "weekend_used" to getWeekendMinutesUsed(),
            "total_minutes" to totalMinutes,
            "tasks_completed" to getTotalGoalsCompleted()
        )
    }

    fun resetDaily() {
        prefs.edit().apply {
            putInt(KEY_WEEKLY_POINTS, 0)
            putInt(KEY_WEEKEND_MINUTES_USED, 0)
            putInt(KEY_CURRENT_STREAK, 0)
            val calendar = Calendar.getInstance()
            calendar.firstDayOfWeek = Calendar.MONDAY
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            for (i in 0..6) {
                val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                remove(KEY_DAILY_POINTS_PREFIX + dateString)
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            apply()
        }
    }
}
