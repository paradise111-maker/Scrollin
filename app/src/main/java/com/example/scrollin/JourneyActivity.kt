package com.example.scrollin

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

class JourneyActivity : AppCompatActivity() {

    private lateinit var pointsManager: PointsManager
    private lateinit var levelManager: UserLevelManager
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var tvUserLevel: TextView
    private lateinit var tvLevelTitle: TextView
    private lateinit var tvPointsToNextLevel: TextView
    private lateinit var konfettiView: KonfettiView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey_enhanced)

        pointsManager = PointsManager(this)
        levelManager = UserLevelManager(this)

        setupViews()
        setupTabs()
        loadUserProgress()

        findViewById<FloatingActionButton>(R.id.fabAddGoal).setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
    }

    private fun setupViews() {
        progressIndicator = findViewById(R.id.levelProgress)
        tvUserLevel = findViewById(R.id.tvUserLevel)
        tvLevelTitle = findViewById(R.id.tvLevelTitle)
        tvPointsToNextLevel = findViewById(R.id.tvPointsToNextLevel)
        konfettiView = findViewById(R.id.konfettiView)
        
        findViewById<TextView>(R.id.tvStreakValue)
        findViewById<TextView>(R.id.tvWeeklyPointsValue)
        findViewById<TextView>(R.id.tvEarnedTimeValue)
    }

    private fun setupTabs() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        
        viewPager.adapter = JourneyPagerAdapter(this)
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when(position) {
                0 -> "📋 Tasks"
                1 -> "📈 Progress"
                2 -> "🏆 Badges"
                else -> ""
            }
        }.attach()
    }

    private fun loadUserProgress() {
        val currentLevel = levelManager.getCurrentLevel()
        val totalPoints = pointsManager.getTotalPoints()
        val progressPercent = levelManager.getLevelProgress()

        ObjectAnimator.ofInt(progressIndicator, "progress", 0, progressPercent)
            .setDuration(1000)
            .start()

        tvUserLevel.text = "Level ${currentLevel.level}"
        tvLevelTitle.text = currentLevel.title

        val pointsToNext = (currentLevel.maxPoints - totalPoints).coerceAtLeast(0)
        tvPointsToNextLevel.text = "$pointsToNext pts to next level"

        updateStatsCards()
    }

    private fun updateStatsCards() {
        val stats = pointsManager.getStatistics()

        findViewById<TextView>(R.id.tvStreakValue).text = "🔥 ${stats["current_streak"]} days"
        findViewById<TextView>(R.id.tvWeeklyPointsValue).text = "⭐ ${stats["weekly_points"]} pts"

        val minutes = stats["weekend_minutes"] as Int
        findViewById<TextView>(R.id.tvEarnedTimeValue).text = "⏰ ${minutes / 60}h ${minutes % 60}m"
    }

    override fun onResume() {
        super.onResume()
        loadUserProgress()
    }

    inner class JourneyPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = 3
        
        override fun createFragment(position: Int): Fragment {
            return when(position) {
                0 -> TasksFragment()
                1 -> ProgressFragment()
                2 -> BadgesFragment()
                else -> TasksFragment()
            }
        }
    }

    fun showCompletionCelebration(goal: JourneyGoal, points: Int) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))

        konfettiView.start(
            Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(0xFF00FF88, 0xFFA16EFF, 0xFF00D4FF).map { it.toInt() },
                emitter = Emitter(duration = 2000, TimeUnit.MILLISECONDS).max(100),
                position = Position.Relative(0.5, 0.5)
            )
        )

        Snackbar.make(
            findViewById(R.id.coordinator),
            "🎉 +$points points earned for completing '${goal.title}'!",
            Snackbar.LENGTH_LONG
        ).show()

        // Check for level up
        val oldLevel = levelManager.getCurrentLevel()
        val newLevel = levelManager.getCurrentLevel()
        
        if (newLevel.level > oldLevel.level) {
            showLevelUpDialog(newLevel)
        }
        
        loadUserProgress()
    }

    private fun showLevelUpDialog(newLevel: UserLevel) {
        MaterialAlertDialogBuilder(this)
            .setTitle("🎊 Level Up!")
            .setMessage("You've reached Level ${newLevel.level}: ${newLevel.title}!\n\nUnlocked perks:\n${newLevel.perks.joinToString("\n") { "• $it" }}")
            .setPositiveButton("Awesome!") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}
