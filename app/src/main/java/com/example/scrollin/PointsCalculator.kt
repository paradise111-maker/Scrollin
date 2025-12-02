package com.example.scrollin

class PointsCalculator(private val userLevel: Int) {

    fun calculatePoints(
        basePoints: Int,
        difficulty: Difficulty,
        streak: Int,
        isFirstTimeToday: Boolean,
        category: Category
    ): Int {
        var points = basePoints

        // 1. Difficulty multiplier
        points = (points * difficulty.multiplier).toInt()

        // 2. Level scaling (activities become less rewarding as you level up)
        val levelPenalty = when {
            userLevel <= 2 -> 1.0f
            userLevel <= 5 -> 0.9f
            userLevel <= 10 -> 0.8f
            else -> 0.7f
        }
        points = (points * levelPenalty).toInt()

        // 3. Streak bonus
        val streakBonus = when {
            streak >= 30 -> 1.5f
            streak >= 14 -> 1.3f
            streak >= 7 -> 1.2f
            streak >= 3 -> 1.1f
            else -> 1.0f
        }
        points = (points * streakBonus).toInt()

        // 4. First activity of the day bonus
        if (isFirstTimeToday) {
            points += 10
        }

        return points.coerceAtLeast(5) // Minimum 5 points
    }

    fun convertPointsToMinutes(points: Int, streak: Int): Int {
        // Base conversion: 10 points = 1 minute
        var minutes = points / 10

        // Streak bonuses
        val streakBonus = when {
            streak >= 30 -> 20
            streak >= 14 -> 10
            streak >= 7 -> 5
            else -> 0
        }

        return minutes + streakBonus
    }
}
