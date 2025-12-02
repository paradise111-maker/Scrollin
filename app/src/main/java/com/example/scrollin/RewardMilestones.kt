package com.example.scrollin

data class Milestone(
    val pointsRequired: Int,
    val reward: Reward
)

sealed class Reward {
    data class UnlockActivity(val activityName: String) : Reward()
    data class UnlockBadge(val badge: JourneyBadge) : Reward()
    data class BonusMinutes(val minutes: Int) : Reward()
    data class UnlockTheme(val themeName: String) : Reward()
}

object RewardMilestones {
    val legendBadge = JourneyBadge("Legend", "Achieved 1000 points", R.drawable.ic_journey, isEarned = false)

    val milestones = listOf(
        Milestone(50, Reward.UnlockActivity("Yoga")),
        Milestone(100, Reward.BonusMinutes(30)),
        Milestone(200, Reward.UnlockActivity("Advanced Meditation")),
        Milestone(350, Reward.UnlockTheme("Ocean Blue")),
        Milestone(500, Reward.BonusMinutes(60)),
        Milestone(750, Reward.UnlockActivity("HIIT Workout")),
        Milestone(1000, Reward.UnlockBadge(legendBadge))
    )
}
