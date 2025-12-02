package com.example.scrollin

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class PointsManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ScrollinPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val userLevelManager = UserLevelManager(context)

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
        private const val KEY_LAST_COMPLETED_CATEGORY = "last_completed_category"
        private const val KEY_LAST_MILESTONE_INDEX = "last_milestone_index"
    }

    fun completeGoal(goal: JourneyGoal): Int {
        val calculator = PointsCalculator(userLevelManager.getCurrentLevel().level)
        val isFirstTimeToday = isFirstActivityToday()

        val earnedPoints = calculator.calculatePoints(
            basePoints = goal.points,
            difficulty = goal.difficulty,
            streak = getCurrentStreak(),
            isFirstTimeToday = isFirstTimeToday,
            category = goal.category
        )

        addPoints(earnedPoints, goal.category)
        updateGoal(goal.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
        return earnedPoints
    }

    fun addPoints(points: Int, category: Category) {
        val oldTotal = getTotalPoints()
        val newTotal = oldTotal + points
        prefs.edit().putInt(KEY_TOTAL_POINTS, newTotal).apply()

        val currentWeekly = getWeeklyPoints()
        prefs.edit().putInt(KEY_WEEKLY_POINTS, currentWeekly + points).apply()

        val today = getTodayDateString()
        val todayKey = KEY_DAILY_POINTS_PREFIX + today
        val todayPoints = prefs.getInt(todayKey, 0)
        prefs.edit().putInt(todayKey, todayPoints + points).apply()

        prefs.edit().putString(KEY_LAST_COMPLETED_CATEGORY, category.name).apply()

        updateStreak()
        checkAndUnlockBadges()
        checkMilestones(oldTotal, newTotal)
    }

    private fun checkMilestones(oldTotal: Int, newTotal: Int) {
        val lastMilestoneIndex = prefs.getInt(KEY_LAST_MILESTONE_INDEX, -1)
        RewardMilestones.milestones.forEachIndexed { index, milestone ->
            if (index > lastMilestoneIndex && newTotal >= milestone.pointsRequired) {
                // Unlock the milestone
                when (val reward = milestone.reward) {
                    is Reward.UnlockActivity -> {
                        // Logic to unlock activity
                        Toast.makeText(context, "New Activity Unlocked: ${reward.activityName}", Toast.LENGTH_LONG).show()
                    }
                    is Reward.UnlockBadge -> {
                        unlockBadge(reward.badge)
                        Toast.makeText(context, "New Badge Unlocked: ${reward.badge.name}", Toast.LENGTH_LONG).show()
                    }
                    is Reward.BonusMinutes -> {
                        // Logic to add bonus minutes
                        Toast.makeText(context, "You earned ${reward.minutes} bonus minutes!", Toast.LENGTH_LONG).show()
                    }
                    is Reward.UnlockTheme -> {
                        // Logic to unlock theme
                        Toast.makeText(context, "New Theme Unlocked: ${reward.themeName}", Toast.LENGTH_LONG).show()
                    }
                }
                prefs.edit().putInt(KEY_LAST_MILESTONE_INDEX, index).apply()
            }
        }
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

    private fun checkAndUnlockBadges() {
        val allBadges = getAllBadges()
        val unlockedBadges = getUnlockedBadges()

        for (badge in allBadges) {
            if (!unlockedBadges.any { it.name == badge.name }) {
                if (checkBadgeCondition(badge)) {
                    unlockBadge(badge)
                }
            }
        }
    }

    private fun checkBadgeCondition(badge: JourneyBadge): Boolean {
        return when (badge.name) {
            "First Steps" -> true
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
        // This should be updated with your new badge system
        return listOf(
            JourneyBadge("First Steps", "Completed 1st activity", R.drawable.ic_journey, false),
            JourneyBadge("Hot Streak", "3-day streak", R.drawable.ic_journey, false),
            JourneyBadge("7-Day Streak", "7-day streak", R.drawable.ic_journey, false)
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
            JourneyGoal(
                id = "morning_meditation", 
                title = "Meditate for 5 minutes", 
                description = "Start your day with a clear mind.",
                points = 10, 
                isCompleted = false, 
                type = GoalType.MORNING,
                category = Category.MENTAL
            ),
            JourneyGoal(
                id = "morning_exercise", 
                title = "Quick 10-min workout",
                description = "Get your body moving.",
                points = 15, 
                isCompleted = false, 
                type = GoalType.MORNING,
                category = Category.PHYSICAL
            ),
            JourneyGoal(
                id = "night_reflect", 
                title = "Journal for 5 minutes",
                description = "Reflect on your day.",
                points = 10, 
                isCompleted = false, 
                type = GoalType.NIGHT,
                category = Category.MENTAL
            ),
            JourneyGoal(
                id = "night_read", 
                title = "Read for 15 minutes", 
                description = "Learn something new before bed.",
                points = 15, 
                isCompleted = false, 
                type = GoalType.NIGHT,
                category = Category.PRODUCTIVITY
            )
        )
    }

    fun updateGoal(updatedGoal: JourneyGoal) {
        val goals = getGoals().toMutableList()
        val index = goals.indexOfFirst { it.id == updatedGoal.id }
        if (index != -1) {
            goals[index] = updatedGoal
            saveGoals(goals)
        }
    }

    private fun saveGoals(goals: List<JourneyGoal>) {
        val json = gson.toJson(goals)
        prefs.edit().putString(KEY_GOALS, json).apply()
    }

    fun getStatistics(): Map<String, Any> {
        val calculator = PointsCalculator(userLevelManager.getCurrentLevel().level)
        val availableMinutes = calculator.convertPointsToMinutes(getWeeklyPoints(), getCurrentStreak())
        val totalMinutes = (getTotalPoints() / 10) * 5

        return mapOf(
            "total_points" to getTotalPoints(),
            "weekly_points" to getWeeklyPoints(),
            "current_streak" to getCurrentStreak(),
            "weekend_minutes" to availableMinutes,
            "weekend_used" to getWeekendMinutesUsed(),
            "total_minutes" to totalMinutes,
            "tasks_completed" to getTotalGoalsCompleted()
        )
    }

    fun resetAllProgress() {
        prefs.edit().clear().apply()
    }

    private fun isFirstActivityToday(): Boolean {
        val today = getTodayDateString()
        val lastActivity = prefs.getString(KEY_LAST_ACTIVITY_DATE, "") ?: ""
        return lastActivity != today
    }

    private fun getWeekendMinutesUsed(): Int {
        return prefs.getInt(KEY_WEEKEND_MINUTES_USED, 0)
    }

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

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getYesterdayDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    fun getTotalPoints(): Int {
        return prefs.getInt(KEY_TOTAL_POINTS, 0)
    }

    fun getWeeklyPoints(): Int {
        checkAndResetWeekly()
        return prefs.getInt(KEY_WEEKLY_POINTS, 0)
    }

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

    private fun getStartOfWeekTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }
}
