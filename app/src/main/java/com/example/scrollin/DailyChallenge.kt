package com.example.scrollin

data class DailyChallenge(
    val id: String,
    val description: String,
    val points: Int,
    var progress: Int = 0,
    var target: Int = 1,
    var isCompleted: Boolean = false,
    var isClaimed: Boolean = false
)
