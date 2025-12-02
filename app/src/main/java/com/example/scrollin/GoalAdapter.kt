package com.example.scrollin

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GoalAdapter(
    private var goals: MutableList<JourneyGoal>,
    private val onGoalCompleted: (JourneyGoal, Boolean) -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(goals[position])
    }

    override fun getItemCount() = goals.size

    fun updateGoals(newGoals: MutableList<JourneyGoal>) {
        this.goals = newGoals
        notifyDataSetChanged()
    }

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvGoalName: TextView = itemView.findViewById(R.id.tvTaskName)
        private val tvGoalPoints: TextView = itemView.findViewById(R.id.tvTaskPoints)
        private val cbGoalCompleted: CheckBox = itemView.findViewById(R.id.cbTaskCompleted)

        fun bind(goal: JourneyGoal) {
            tvGoalName.text = goal.title
            tvGoalPoints.text = "+${goal.points} pts"
            
            cbGoalCompleted.setOnCheckedChangeListener(null)
            cbGoalCompleted.isChecked = goal.isCompleted
            updateGoalAppearance(goal.isCompleted)

            cbGoalCompleted.setOnCheckedChangeListener { _, isChecked ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val currentGoal = goals[adapterPosition]
                    if (currentGoal.isCompleted != isChecked) {
                        onGoalCompleted(currentGoal, isChecked)
                    }
                }
            }
        }

        private fun updateGoalAppearance(isCompleted: Boolean) {
            if (isCompleted) {
                tvGoalName.paintFlags = tvGoalName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                itemView.alpha = 0.5f
            } else {
                tvGoalName.paintFlags = tvGoalName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                itemView.alpha = 1.0f
            }
        }
    }
}
