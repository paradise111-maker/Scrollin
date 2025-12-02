package com.example.scrollin

data class JourneyGoal(
    val id: String, // Unique ID for the goal
    val title: String,
    val points: Int,
    var isCompleted: Boolean = false,
    val type: GoalType // To distinguish between morning, night, etc.
)

enum class GoalType {
    MORNING, NIGHT, GENERAL
}
