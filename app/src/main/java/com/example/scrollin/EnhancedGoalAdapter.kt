package com.example.scrollin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator

class EnhancedGoalAdapter(
    private var goals: List<JourneyGoal>,
    private val onGoalCompleted: (JourneyGoal, Boolean) -> Unit,
    private val onGoalStarted: (JourneyGoal) -> Unit
) : RecyclerView.Adapter<EnhancedGoalAdapter.EnhancedGoalViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnhancedGoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal_enhanced, parent, false)
        return EnhancedGoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: EnhancedGoalViewHolder, position: Int) {
        holder.bind(goals[position])
    }

    override fun getItemCount() = goals.size

    fun updateGoals(newGoals: List<JourneyGoal>) {
        this.goals = newGoals
        notifyDataSetChanged()
    }

    inner class EnhancedGoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryIcon: TextView = itemView.findViewById(R.id.tvCategoryIcon)
        private val tvGoalTitle: TextView = itemView.findViewById(R.id.tvGoalTitle)
        private val chipDifficulty: Chip = itemView.findViewById(R.id.chipDifficulty)
        private val tvGoalDescription: TextView = itemView.findViewById(R.id.tvGoalDescription)
        private val progressGoal: LinearProgressIndicator = itemView.findViewById(R.id.progressGoal)
        private val tvPoints: TextView = itemView.findViewById(R.id.tvPoints)
        private val tvEstimatedTime: TextView = itemView.findViewById(R.id.tvEstimatedTime)
        private val cbComplete: CheckBox = itemView.findViewById(R.id.cbComplete)
        private val btnStartGoal: Button = itemView.findViewById(R.id.btnStartGoal)

        fun bind(goal: JourneyGoal) {
            tvCategoryIcon.text = goal.category.icon
            tvGoalTitle.text = goal.title
            tvGoalDescription.text = goal.description
            tvPoints.text = "+${goal.points} pts"

            val difficulty = goal.difficulty
            chipDifficulty.text = difficulty.name
            val difficultyColor = when (difficulty) {
                Difficulty.EASY -> R.color.difficulty_easy
                Difficulty.MEDIUM -> R.color.difficulty_medium
                Difficulty.HARD -> R.color.difficulty_hard
            }
            chipDifficulty.setChipBackgroundColorResource(difficultyColor)

            if (goal.estimatedTime != null) {
                tvEstimatedTime.text = "~${goal.estimatedTime} min"
                tvEstimatedTime.visibility = View.VISIBLE

                if (!goal.isInProgress && !goal.isCompleted) {
                    btnStartGoal.visibility = View.VISIBLE
                    cbComplete.visibility = View.GONE
                } else {
                    btnStartGoal.visibility = View.GONE
                    cbComplete.visibility = View.VISIBLE
                }

                if (goal.isInProgress) {
                     tvEstimatedTime.text = "In Progress"
                }

            } else {
                tvEstimatedTime.visibility = View.GONE
                btnStartGoal.visibility = View.GONE
                cbComplete.visibility = View.VISIBLE
            }

            if (goal.targetProgress > 1) {
                progressGoal.max = goal.targetProgress
                progressGoal.progress = goal.progress
                progressGoal.visibility = View.VISIBLE
            } else {
                progressGoal.visibility = View.GONE
            }

            cbComplete.setOnCheckedChangeListener(null)
            cbComplete.isChecked = goal.isCompleted

            cbComplete.setOnCheckedChangeListener { _, isChecked ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onGoalCompleted(goals[adapterPosition], isChecked)
                }
            }

            btnStartGoal.setOnClickListener {
                 if (adapterPosition != RecyclerView.NO_POSITION) {
                    onGoalStarted(goals[adapterPosition])
                }
            }
        }
    }
}
