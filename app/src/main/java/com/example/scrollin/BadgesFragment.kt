package com.example.scrollin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BadgesFragment : Fragment() {
    private lateinit var pointsManager: PointsManager
    private lateinit var badgeAdapter: BadgeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_badges, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pointsManager = PointsManager(requireContext())

        val rvBadges = view.findViewById<RecyclerView>(R.id.rvBadges)
        val allBadges = getAllBadgesWithStatus()
        
        badgeAdapter = BadgeAdapter(allBadges)
        rvBadges.adapter = badgeAdapter
        rvBadges.layoutManager = GridLayoutManager(context, 2)
    }

    private fun getAllBadgesWithStatus(): List<JourneyBadge> {
        val unlockedBadges = pointsManager.getUnlockedBadges()
        val allPossibleBadges = listOf(
            JourneyBadge("First Steps", "Complete your first activity", R.drawable.ic_journey, false),
            JourneyBadge("Hot Streak", "Maintain a 3-day streak", R.drawable.ic_journey, false),
            JourneyBadge("7-Day Warrior", "Complete 7 days in a row", R.drawable.ic_journey, false),
            JourneyBadge("Taskmaster", "Complete 10 tasks", R.drawable.ic_journey, false),
            JourneyBadge("Goal-Getter", "Complete 50 tasks", R.drawable.ic_journey, false),
            JourneyBadge("Zen Novice", "Meditate for 60 minutes", R.drawable.ic_journey, false),
            JourneyBadge("Zen Master", "Meditate for 600 minutes", R.drawable.ic_journey, false),
            JourneyBadge("Fitness Fiend", "Complete 25 exercises", R.drawable.ic_journey, false),
            JourneyBadge("Workout Warrior", "Complete 100 exercises", R.drawable.ic_journey, false),
            JourneyBadge("Early Bird", "Complete 20 morning activities", R.drawable.ic_journey, false),
            JourneyBadge("Night Owl", "Complete 20 night activities", R.drawable.ic_journey, false),
            JourneyBadge("Point Master", "Earn 1000 total points", R.drawable.ic_journey, false),
            JourneyBadge("Legend", "Reach Level 5", R.drawable.ic_journey, false)
        )

        return allPossibleBadges.map { badge ->
            badge.copy(isEarned = unlockedBadges.any { it.name == badge.name })
        }
    }

    override fun onResume() {
        super.onResume()
        badgeAdapter.updateBadges(getAllBadgesWithStatus())
    }
}