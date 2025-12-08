package com.example.scrollin

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator

class BadgesFragment : Fragment() {
    private lateinit var pointsManager: PointsManager
    private lateinit var badgeAdapter: BadgeAdapter
    private lateinit var tvBadgeProgress: TextView
    private lateinit var progressBadges: LinearProgressIndicator

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
        tvBadgeProgress = view.findViewById(R.id.tvBadgeProgress)
        progressBadges = view.findViewById(R.id.progressBadges)
        
        val allBadges = getAllBadgesWithStatus()
        
        badgeAdapter = BadgeAdapter(allBadges)
        rvBadges.adapter = badgeAdapter
        rvBadges.layoutManager = GridLayoutManager(context, 2)
        
        updateProgressIndicator(allBadges)
    }

    private fun updateProgressIndicator(badges: List<JourneyBadge>) {
        val earnedCount = badges.count { it.isEarned }
        val totalCount = badges.size
        val percentage = ((earnedCount.toFloat() / totalCount) * 100).toInt()
        
        tvBadgeProgress.text = "$earnedCount/$totalCount badges earned"
        
        // Animate progress bar
        ObjectAnimator.ofInt(progressBadges, "progress", 0, percentage)
            .setDuration(1000)
            .start()
    }

    private fun getAllBadgesWithStatus(): List<JourneyBadge> {
        val unlockedBadges = pointsManager.getUnlockedBadges()
        val stats = pointsManager.getStatistics()
        val totalPoints = stats["total_points"] as? Int ?: 0
        val currentStreak = stats["current_streak"] as? Int ?: 0
        
        val allPossibleBadges = listOf(
            // Beginner Badges
            JourneyBadge(
                "First Steps", 
                "Complete your first activity", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Getting Started", 
                "Earn 50 total points", 
                R.drawable.ic_journey, 
                false
            ),
            
            // Streak Badges
            JourneyBadge(
                "Hot Streak", 
                "Maintain a 3-day streak", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Week Warrior", 
                "Complete 7 days in a row", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Fortnight Fighter", 
                "Maintain a 14-day streak", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Monthly Master", 
                "Complete 30 days in a row", 
                R.drawable.ic_journey, 
                false
            ),
            
            // Task Completion Badges
            JourneyBadge(
                "Task Beginner", 
                "Complete 5 tasks", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Taskmaster", 
                "Complete 25 tasks", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Goal-Getter", 
                "Complete 50 tasks", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Century Club", 
                "Complete 100 tasks", 
                R.drawable.ic_journey, 
                false
            ),
            
            // Meditation Badges
            JourneyBadge(
                "Mindful Beginner", 
                "Meditate for 30 minutes total", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Zen Novice", 
                "Meditate for 60 minutes total", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Meditation Master", 
                "Meditate for 300 minutes total", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Zen Legend", 
                "Meditate for 600 minutes total", 
                R.drawable.ic_journey, 
                false
            ),
            
            // Fitness Badges
            JourneyBadge(
                "Fitness Starter", 
                "Complete 10 exercises", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Fitness Fiend", 
                "Complete 50 exercises", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Workout Warrior", 
                "Complete 100 exercises", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Fitness Legend", 
                "Complete 250 exercises", 
                R.drawable.ic_journey, 
                false
            ),
            
            // Time Period Badges
            JourneyBadge(
                "Early Bird", 
                "Complete 20 morning activities", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Night Owl", 
                "Complete 20 night activities", 
                R.drawable.ic_journey, 
                false
            ),
            
            // Points Milestones
            JourneyBadge(
                "Point Collector", 
                "Earn 250 total points", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Point Master", 
                "Earn 500 total points", 
                R.drawable.ic_journey, 
                false
            ),
            JourneyBadge(
                "Point Champion", 
                "Earn 1000 total points", 
                R.drawable.ic_journey, 
                false
            ),
            
            // Ultimate Badge
            JourneyBadge(
                "Scrollin Legend", 
                "Reach Level 5 and 1000 points", 
                R.drawable.ic_journey, 
                false
            )
        )

        return allPossibleBadges.map { badge ->
            badge.copy(isEarned = unlockedBadges.any { it.name == badge.name })
        }
    }

    override fun onResume() {
        super.onResume()
        val updatedBadges = getAllBadgesWithStatus()
        badgeAdapter.updateBadges(updatedBadges)
        updateProgressIndicator(updatedBadges)
    }
}