package com.example.scrollin

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvGreetingSubtitle: TextView
    private lateinit var tvWeeklyPoints: TextView
    private lateinit var tvStreak: TextView
    private lateinit var tvWeekendTime: TextView
    private lateinit var pointsManager: PointsManager
    private lateinit var lineChart: LineChart

    // Views for animation
    private lateinit var headerLayout: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var bottomNavigation: BottomNavigationView

    // Clickable cards
    private lateinit var morningRitualsCard: LinearLayout
    private lateinit var nightWindDownCard: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pointsManager = PointsManager(this)

        // Initialize views
        tvGreeting = findViewById(R.id.tvGreeting)
        tvGreetingSubtitle = findViewById(R.id.tvGreetingSubtitle)
        tvWeeklyPoints = findViewById(R.id.tvWeeklyPoints)
        tvStreak = findViewById(R.id.tvStreak)
        tvWeekendTime = findViewById(R.id.tvWeekendTime)
        scrollView = findViewById(R.id.scrollView)
        headerLayout = findViewById(R.id.headerLayout)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        lineChart = findViewById(R.id.lineChart)
        morningRitualsCard = findViewById(R.id.morning_rituals_card)
        nightWindDownCard = findViewById(R.id.night_wind_down_card)

        setupClickListeners()
        setupParallaxScrolling()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        runEntranceAnimation()
        bottomNavigation.selectedItemId = R.id.nav_dashboard // Fix for stuck icon
    }

    private fun setupClickListeners() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_activities -> {
                    startActivity(Intent(this, ActivitySelectionActivity::class.java))
                    true
                }
                R.id.nav_perks -> {
                    Toast.makeText(this, "Perks screen coming soon!", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        morningRitualsCard.setOnClickListener {
            val intent = Intent(this, ActivitySelectionActivity::class.java)
            intent.putExtra("ACTIVITY_TYPE", "MORNING")
            startActivity(intent)
        }

        nightWindDownCard.setOnClickListener {
            val intent = Intent(this, ActivitySelectionActivity::class.java)
            intent.putExtra("ACTIVITY_TYPE", "NIGHT")
            startActivity(intent)
        }
    }

    private fun setupParallaxScrolling() {
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            headerLayout.translationY = scrollY * 0.5f
        }
    }

    private fun updateUI() {
        val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
        val userName = prefs.getString("user_name", "")
        val greetingName = if (userName?.isNotBlank() == true) userName else "User"

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = when (hour) {
            in 0..11 -> "Good Morning, $greetingName!"
            in 12..16 -> "Good Afternoon, $greetingName!"
            else -> "Good Evening, $greetingName!"
        }

        val stats = pointsManager.getStatistics()
        val weeklyPoints = stats["weekly_points"] as? Int ?: 0
        tvWeeklyPoints.text = "$weeklyPoints"

        val streak = stats["current_streak"] as? Int ?: 0
        tvStreak.text = "$streak days"

        val weekendMinutes = stats["weekend_minutes"] as? Int ?: 0
        val hours = weekendMinutes / 60
        val minutes = weekendMinutes % 60
        tvWeekendTime.text = if (hours > 0) "${hours}h ${minutes}m" else "$minutes min"

        updateBlockStatus()
        setupChart()
    }

    private fun setupChart() {
        // Get REAL data from PointsManager
        val pointsData = pointsManager.getLast7DaysPoints()
        
        val entries = ArrayList<Entry>()
        for ((index, points) in pointsData.withIndex()) {
            entries.add(Entry(index.toFloat(), points.toFloat()))
        }
        
        // If no data yet, show empty chart
        if (entries.all { it.y == 0f }) {
            // Add placeholder data to show chart structure
            entries.clear()
            for (i in 0..6) {
                entries.add(Entry(i.toFloat(), 0f))
            }
        }

        val dataSet = LineDataSet(entries, "Weekly Progress").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.neon_green)
            valueTextColor = Color.WHITE
            setDrawValues(false)
            lineWidth = 3f
            setDrawCircles(true)
            circleRadius = 4f
            setCircleColor(ContextCompat.getColor(this@MainActivity, R.color.neon_green))
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_chart_fill)
        }

        lineChart.apply {
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
            invalidate()
        }
    }
    
    private fun runEntranceAnimation() {
        val viewsToAnimate = listOf(
            headerLayout,
            (tvWeeklyPoints.parent.parent.parent as View), // Points & Stats Card
            findViewById(R.id.activities_today_title),
            findViewById(R.id.morning_rituals_card),
            findViewById(R.id.night_wind_down_card),
            bottomNavigation
        )

        for ((index, view) in viewsToAnimate.withIndex()) {
            view.alpha = 0f
            view.translationY = 100f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 80).toLong()) 
                .setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun updateBlockStatus() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
        val wakeHour = prefs.getInt("wake_hour", 7)

        val morningBlockEnd = (wakeHour + 3) % 24
        val isInMorningBlock = hour in wakeHour until morningBlockEnd
        val isInNightBlock = hour in 19..23

        when {
            isInMorningBlock -> {
                tvGreetingSubtitle.text = "🌅 Morning Block Active"
                tvGreetingSubtitle.setTextColor(getColor(R.color.neon_yellow))
            }
            isInNightBlock -> {
                tvGreetingSubtitle.text = "🌙 Night Block Active"
                tvGreetingSubtitle.setTextColor(getColor(R.color.neon_purple))
            }
            pointsManager.isWeekend() -> {
                val available = pointsManager.getAvailableWeekendMinutes()
                tvGreetingSubtitle.text = "🎉 Weekend Time! ($available min left)"
                tvGreetingSubtitle.setTextColor(getColor(R.color.neon_green))
            }
            else -> {
                tvGreetingSubtitle.text = "✅ Free Time"
                tvGreetingSubtitle.setTextColor(getColor(R.color.neon_green))
            }
        }
    }

    inner class DayAxisValueFormatter : ValueFormatter() {
        private val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
            return days.getOrNull(value.toInt()) ?: ""
        }
    }
}