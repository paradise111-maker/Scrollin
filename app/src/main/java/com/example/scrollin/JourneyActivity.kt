package com.example.scrollin

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

class JourneyActivity : AppCompatActivity() {

    private lateinit var pointsManager: PointsManager
    private lateinit var levelManager: UserLevelManager
    private lateinit var challengeManager: DailyChallengeManager
    private lateinit var goalSuggestionEngine: GoalSuggestionEngine
    private lateinit var goalAdapter: EnhancedGoalAdapter
    private lateinit var suggestedGoalAdapter: EnhancedGoalAdapter
    private lateinit var badgeAdapter: BadgeAdapter

    // Views
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var tvUserLevel: TextView
    private lateinit var tvLevelTitle: TextView
    private lateinit var tvPointsToNextLevel: TextView
    private lateinit var konfettiView: KonfettiView
    private lateinit var progressChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey_enhanced)

        pointsManager = PointsManager(this)
        levelManager = UserLevelManager(this)
        challengeManager = DailyChallengeManager(this)
        goalSuggestionEngine = GoalSuggestionEngine(pointsManager)

        setupViews()
        setupRecyclerViews()
        loadUserProgress()

        findViewById<FloatingActionButton>(R.id.fabAddGoal).setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserProgress()
    }

    private fun setupViews() {
        progressIndicator = findViewById(R.id.levelProgress)
        tvUserLevel = findViewById(R.id.tvUserLevel)
        tvLevelTitle = findViewById(R.id.tvLevelTitle)
        tvPointsToNextLevel = findViewById(R.id.tvPointsToNextLevel)
        konfettiView = findViewById(R.id.konfettiView)
        progressChart = findViewById(R.id.progressChart)

        val appBar = findViewById<AppBarLayout>(R.id.appBar)
        appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val percentage = Math.abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange
            progressIndicator.alpha = 1 - percentage
        }
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
        setupDailyChallenge()
        setupProgressChart()
        setupSuggestedGoals()
        goalAdapter.updateGoals(pointsManager.getGoals())
        badgeAdapter.updateBadges(pointsManager.getUnlockedBadges().takeLast(3))
    }

    private fun updateStatsCards() {
        val stats = pointsManager.getStatistics()

        findViewById<TextView>(R.id.tvStreakValue).text = "${stats["current_streak"]} days"
        findViewById<TextView>(R.id.tvWeeklyPointsValue).text = "${stats["weekly_points"]} pts"

        val minutes = stats["weekend_minutes"] as Int
        findViewById<TextView>(R.id.tvEarnedTimeValue).text = "${minutes / 60}h ${minutes % 60}m"
    }

    private fun setupDailyChallenge() {
        val challenge = challengeManager.getTodaysChallenge()
        val challengeCard = findViewById<MaterialCardView>(R.id.challengeCard)

        findViewById<TextView>(R.id.tvChallengeDescription).text = challenge.description
        findViewById<TextView>(R.id.tvChallengePoints).text = "+${challenge.points} pts"
        findViewById<ProgressBar>(R.id.progressChallenge).apply {
            max = challenge.target
            progress = challenge.progress
        }

        val btnClaim = findViewById<Button>(R.id.btnClaimChallenge)
        if (challenge.isCompleted) {
            challengeCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.success_light))
            btnClaim.text = if (challenge.isClaimed) "Claimed! ✓" else "Claim Reward"
            btnClaim.isEnabled = !challenge.isClaimed
        } else {
            btnClaim.text = "In Progress"
            btnClaim.isEnabled = false
        }

        btnClaim.setOnClickListener {
            val points = challengeManager.claimChallengeReward()
            if (points > 0) {
                Snackbar.make(findViewById(R.id.coordinator), "+${points} for the daily challenge!", Snackbar.LENGTH_LONG).show()
                loadUserProgress()
            }
        }
    }

    private fun setupRecyclerViews() {
        val rvActiveGoals = findViewById<RecyclerView>(R.id.rvActiveGoals)
        goalAdapter = EnhancedGoalAdapter(pointsManager.getGoals()) { goal, isChecked ->
            if (isChecked) {
                handleGoalCompletion(goal)
            }
        }
        rvActiveGoals.adapter = goalAdapter
        rvActiveGoals.layoutManager = LinearLayoutManager(this)

        val rvSuggestedGoals = findViewById<RecyclerView>(R.id.rvSuggestedGoals)
        suggestedGoalAdapter = EnhancedGoalAdapter(emptyList()) { goal, isChecked ->
            if (isChecked) {
                pointsManager.addGoal(goal)
                handleGoalCompletion(goal)
            }
        }
        rvSuggestedGoals.adapter = suggestedGoalAdapter
        rvSuggestedGoals.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val rvRecentBadges = findViewById<RecyclerView>(R.id.rvRecentBadges)
        badgeAdapter = BadgeAdapter(pointsManager.getUnlockedBadges().takeLast(3))
        rvRecentBadges.adapter = badgeAdapter
        rvRecentBadges.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupProgressChart() {
        val pointsData = pointsManager.getLast7DaysPoints()

        val entries = pointsData.mapIndexed { index, points ->
            Entry(index.toFloat(), points.toFloat())
        }

        val dataSet = LineDataSet(entries, "Daily Points").apply {
            color = ContextCompat.getColor(this@JourneyActivity, R.color.neon_green)
            lineWidth = 3f
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@JourneyActivity, R.color.neon_green)
            fillAlpha = 50
            setDrawCircles(true)
            circleRadius = 5f
            setCircleColor(ContextCompat.getColor(this@JourneyActivity, R.color.neon_green))
            mode = LineDataSet.Mode.CUBIC_BEZIER
            valueTextColor = Color.WHITE
            setDrawValues(false)
        }

        progressChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.GRAY
                valueFormatter = DayAxisValueFormatter()
            }
            setTouchEnabled(false)
            animateX(1000)
            invalidate()
        }
    }

    private fun setupSuggestedGoals() {
        val suggestedGoals = goalSuggestionEngine.getSuggestedGoals(
            userLevel = levelManager.getCurrentLevel().level,
            completedGoals = pointsManager.getGoals().filter { it.isCompleted }
        )
        suggestedGoalAdapter.updateGoals(suggestedGoals)
    }

    private fun handleGoalCompletion(goal: JourneyGoal) {
        val oldLevel = levelManager.getCurrentLevel()
        val earnedPoints = pointsManager.completeGoal(goal)
        val newLevel = levelManager.getCurrentLevel()

        showCompletionCelebration(goal, earnedPoints)

        if (newLevel.level > oldLevel.level) {
            showLevelUpDialog(newLevel)
        }

        // Update daily challenge progress
        val challenge = challengeManager.getTodaysChallenge()
        if (!challenge.isCompleted) {
            challengeManager.updateChallengeProgress(challenge.progress + 1)
        }

        loadUserProgress()
    }

    private fun showCompletionCelebration(goal: JourneyGoal, points: Int) {
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

    inner class DayAxisValueFormatter : ValueFormatter() {
        private val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
            return days.getOrNull(value.toInt()) ?: ""
        }
    }
}
