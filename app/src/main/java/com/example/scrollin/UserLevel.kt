package com.example.scrollin

data class UserLevel(
    val level: Int,
    val title: String,
    val minPoints: Int,
    val maxPoints: Int,
    val perks: List<String>
)
