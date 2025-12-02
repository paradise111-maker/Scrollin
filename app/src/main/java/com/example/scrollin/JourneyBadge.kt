package com.example.scrollin

import androidx.annotation.DrawableRes

data class JourneyBadge(
    val name: String,
    val description: String,
    @DrawableRes val iconResId: Int,
    var isEarned: Boolean = false
)
