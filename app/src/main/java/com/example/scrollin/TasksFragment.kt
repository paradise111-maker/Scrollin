package com.example.scrollin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TasksFragment : Fragment() {
    private lateinit var pointsManager: PointsManager
    private lateinit var goalAdapter: EnhancedGoalAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pointsManager = PointsManager(requireContext())
        
        val rvTasks = view.findViewById<RecyclerView>(R.id.rvTasks)
        goalAdapter = EnhancedGoalAdapter(pointsManager.getGoals()) { goal, isChecked ->
            if (isChecked) {
                // Handle task completion with timer validation
                handleTaskCompletion(goal)
            }
        }
        rvTasks.adapter = goalAdapter
        rvTasks.layoutManager = LinearLayoutManager(context)
    }
    
    private fun handleTaskCompletion(goal: JourneyGoal) {
        if (goal.isInProgress && goal.startTime != null) {
            val elapsed = (System.currentTimeMillis() - (goal.startTime ?: 0)) / 60000 // minutes
            val required = goal.estimatedTime ?: 0
            
            if (elapsed >= required) {
                pointsManager.completeGoal(goal)
            } else {
                // Task failed - not completed in time
                // Reset the task
                val updatedGoal = goal.copy(isInProgress = false, startTime = null)
                pointsManager.updateGoal(updatedGoal)
            }
        }
    }
}