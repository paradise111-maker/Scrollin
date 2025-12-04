package com.example.scrollin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BadgeAdapter(private var badges: List<JourneyBadge>) : 
    RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge_enhanced, parent, false)
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
        private val ivLock: ImageView = itemView.findViewById(R.id.ivLockIcon)

        fun bind(badge: JourneyBadge) {
            ivIcon.setImageResource(badge.iconResId)
            tvTitle.text = badge.name
            tvDescription.text = badge.description

            if (badge.isEarned) {
                itemView.alpha = 1.0f
                ivLock.visibility = View.GONE
            } else {
                itemView.alpha = 0.4f
                ivLock.visibility = View.VISIBLE
            }
        }
    }
}