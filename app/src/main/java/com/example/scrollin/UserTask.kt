package com.example.scrollin

data class UserTask(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val points: Int,
    val duration: Int, // in minutes
    var isCompleted: Boolean = false
)
