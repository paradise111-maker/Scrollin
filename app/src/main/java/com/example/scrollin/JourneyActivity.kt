package com.example.scrollin

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class JourneyActivity : AppCompatActivity() {

    private lateinit var tvTotalEarnedTime: TextView
    private lateinit var tvDoomscrollingAvoided: TextView
    private lateinit var tvTotalPoints: TextView
    private lateinit var rvBadges: RecyclerView
    private lateinit var rvGoals: RecyclerView
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var pointsManager: PointsManager
    private lateinit var goalAdapter: GoalAdapter
    private lateinit var badgeAdapter: BadgeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey)

        pointsManager = PointsManager(this)

        tvTotalEarnedTime = findViewById(R.id.tvTotalEarnedTime)
        tvDoomscrollingAvoided = findViewById(R.id.tvDoomscrollingAvoided)
        tvTotalPoints = findViewById(R.id.tvTotalPoints)
        rvBadges = findViewById(R.id.rvBadges)
        rvGoals = findViewById(R.id.rvTasks) // rvTasks is now rvGoals
        fabAddTask = findViewById(R.id.fabAddTask)

        setupAdapters()
        setupRecyclerViews()
        refreshData()

        fabAddTask.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun setupStats() {
        val stats = pointsManager.getStatistics()

        val totalMinutes = stats["total_minutes"] as? Int ?: 0
        val totalHours = totalMinutes / 60
        val remainingMinutes = totalMinutes % 60
        tvTotalEarnedTime.text = "${totalHours}h ${remainingMinutes}m"

        // Assuming a 1:1 ratio for now
        tvDoomscrollingAvoided.text = "${totalHours}h ${remainingMinutes}m"

        val totalPoints = stats["total_points"] as? Int ?: 0
        tvTotalPoints.text = totalPoints.toString()
    }

    private fun setupAdapters() {
        badgeAdapter = BadgeAdapter(emptyList())
        goalAdapter = GoalAdapter(mutableListOf()) { goal, isChecked ->
            onGoalCompleted(goal, isChecked)
        }
    }

    private fun setupRecyclerViews() {
        rvBadges.layoutManager = GridLayoutManager(this, 3)
        rvBadges.adapter = badgeAdapter

        rvGoals.layoutManager = LinearLayoutManager(this)
        rvGoals.adapter = goalAdapter
    }

    private fun refreshData() {
        // Refresh badges
        val allBadges = pointsManager.getAllBadges()
        val unlockedBadges = pointsManager.getUnlockedBadges()
        val badgeList = allBadges.map { badge ->
            badge.copy(isEarned = unlockedBadges.any { it.name == badge.name })
        }
        badgeAdapter.updateBadges(badgeList)

        // Refresh goals
        val goals = pointsManager.getGoals().toMutableList()
        goalAdapter.updateGoals(goals)

        // Refresh stats
        setupStats()
    }

    private fun onGoalCompleted(goal: JourneyGoal, isChecked: Boolean) {
        val updatedGoal = goal.copy(isCompleted = isChecked)
        pointsManager.updateGoal(updatedGoal)

        if (isChecked) {
            Toast.makeText(this, "+${goal.points} points for completing a goal!", Toast.LENGTH_SHORT).show()
        }

        // Refresh stats and badges after goal completion
        refreshData()
    }
}
