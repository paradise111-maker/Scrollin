package com.example.scrollin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BadgeAdapter(private var badges: List<JourneyBadge>) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(badges[position])
    }

    override fun getItemCount() = badges.size

    fun updateBadges(newBadges: List<JourneyBadge>) {
        this.badges = newBadges
        notifyDataSetChanged()
    }

    class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivBadgeIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvBadgeTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvBadgeDescription)

        fun bind(badge: JourneyBadge) {
            ivIcon.setImageResource(badge.iconResId)
            tvTitle.text = badge.name
            tvDescription.text = badge.description

            // Set opacity based on whether the badge is earned
            itemView.alpha = if (badge.isEarned) 1.0f else 0.5f
        }
    }
}
