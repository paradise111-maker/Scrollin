package com.example.scrollin

data class JourneyGoal(
    val id: String,
    val title: String,
    val description: String? = null,
    val points: Int,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val category: Category = Category.GENERAL,
    val estimatedTime: Int? = null, // in minutes
    val deadline: Long? = null,
    val repeatType: RepeatType = RepeatType.ONCE,
    var progress: Int = 0,
    var targetProgress: Int = 1,
    var isCompleted: Boolean = false,
    var isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val type: GoalType, // MORNING, NIGHT, GENERAL
    var isInProgress: Boolean = false, // NEW: Track if task is currently running
    var startTime: Long? = null // NEW: Track when task was started
)

enum class Difficulty(val multiplier: Float) {
    EASY(1.0f),
    MEDIUM(1.5f),
    HARD(2.0f)
}

enum class Category(val icon: String, val color: Int) {
    PHYSICAL("💪", 0xFF00FF88.toInt()),
    MENTAL("🧠", 0xFFA16EFF.toInt()),
    PRODUCTIVITY("✅", 0xFF00D4FF.toInt()),
    GENERAL("⭐", 0xFFFFFFFF.toInt())
}

enum class RepeatType {
    ONCE, DAILY, WEEKLY, CUSTOM
}

enum class GoalType {
    MORNING, NIGHT, GENERAL
}
