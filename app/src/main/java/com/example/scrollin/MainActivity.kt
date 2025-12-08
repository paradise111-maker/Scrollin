package com.example.scrollin

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvGreetingSubtitle: TextView
    private lateinit var tvWeeklyPoints: TextView
    private lateinit var tvStreak: TextView
    private lateinit var tvWeekendTime: TextView
    private lateinit var pointsManager: PointsManager
    private lateinit var lineChart: LineChart
    private lateinit var ivSettings: ImageView
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var headerLayout: RelativeLayout
    private lateinit var scrollView: ScrollView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var morningRitualsCard: LinearLayout
    private lateinit var nightWindDownCard: LinearLayout
    
    // NEW: Weekend time indicator
    private lateinit var tvWeekendTimeStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pointsManager = PointsManager(this)

        // Schedule daily reset
        DailyResetWorker.schedule(this)

        tvGreeting = findViewById(R.id.tvGreeting)
        tvGreetingSubtitle = findViewById(R.id.tvGreetingSubtitle)
        tvWeeklyPoints = findViewById(R.id.tvWeeklyPoints)
        tvStreak = findViewById(R.id.tvStreak)
        tvWeekendTime = findViewById(R.id.tvWeekendTime)
        tvWeekendTimeStatus = findViewById(R.id.tvWeekendTimeStatus)
        scrollView = findViewById(R.id.scrollView)
        headerLayout = findViewById(R.id.headerLayout)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        lineChart = findViewById(R.id.lineChart)
        morningRitualsCard = findViewById(R.id.morning_rituals_card)
        nightWindDownCard = findViewById(R.id.night_wind_down_card)
        ivSettings = findViewById(R.id.ivSettings)
        fabAddTask = findViewById(R.id.fabAddTask)

        setupClickListeners()
        setupParallaxScrolling()
        
        // Check accessibility service status on startup
        checkAccessibilityServiceStatus()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        runEntranceAnimation()
        bottomNavigation.selectedItemId = R.id.nav_dashboard
        checkAccessibilityServiceStatus()
    }

    private fun checkAccessibilityServiceStatus() {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            ) == 1
        } catch (e: Exception) {
            false
        }

        val serviceName = "$packageName/.DoomscrollAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""

        val isServiceEnabled = enabledServices.contains(serviceName)

        if (!isServiceEnabled && !accessibilityEnabled) {
            showAccessibilityWarning()
        }
    }

    private fun showAccessibilityWarning() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Setup Required")
            .setMessage("Scrollin needs accessibility permission to block distracting apps. Enable it now?")
            .setPositiveButton("Enable") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(
                    this,
                    "Find and enable 'Scrollin' in the list",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun setupClickListeners() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_journey -> {
                    startActivity(Intent(this, JourneyActivity::class.java))
                    true
                }
                else -> false
            }
        }

        ivSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
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

        fabAddTask.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
        
        // NEW: Click on weekend time to see details
        tvWeekendTime.setOnClickListener {
            showWeekendTimeDetails()
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
        val usedMinutesToday = prefs.getInt("weekend_minutes_used_today", 0)
        val remainingMinutes = (weekendMinutes - usedMinutesToday).coerceAtLeast(0)
        
        val hours = remainingMinutes / 60
        val minutes = remainingMinutes % 60
        tvWeekendTime.text = if (hours > 0) "${hours}h ${minutes}m" else "$minutes min"
        
        // NEW: Show usage status
        if (usedMinutesToday > 0) {
            tvWeekendTimeStatus.text = "Used $usedMinutesToday min today"
            tvWeekendTimeStatus.visibility = View.VISIBLE
        } else {
            tvWeekendTimeStatus.visibility = View.GONE
        }

        updateBlockStatus()
        setupChart()
    }

    private fun showWeekendTimeDetails() {
        val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
        val stats = pointsManager.getStatistics()
        val totalEarned = stats["weekend_minutes"] as? Int ?: 0
        val usedToday = prefs.getInt("weekend_minutes_used_today", 0)
        val remaining = (totalEarned - usedToday).coerceAtLeast(0)

        val message = """
            Weekend Time Summary
            
            📊 Total Earned: $totalEarned minutes
            ✅ Used Today: $usedToday minutes
            ⏰ Remaining: $remaining minutes
            
            ${if (remaining > 0) "You can use your earned time during blocked periods!" else "Complete more activities to earn time!"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("🎉 Weekend Time")
            .setMessage(message)
            .setPositiveButton("Got it!", null)
            .show()
    }

    private fun setupChart() {
        val pointsData = pointsManager.getLast7DaysPoints()

        val entries = ArrayList<Entry>()
        for ((index, points) in pointsData.withIndex()) {
            entries.add(Entry(index.toFloat(), points.toFloat()))
        }

        if (entries.all { it.y == 0f }) {
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
            (tvWeeklyPoints.parent.parent.parent as View),
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
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
        val wakeHour = prefs.getInt("wake_hour", 7)

        val morningBlockEnd = (wakeHour + 3) % 24
        val isInMorningBlock = hour in wakeHour until morningBlockEnd
        val isInNightBlock = hour in 19..23
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

        when {
            isInMorningBlock -> {
                tvGreetingSubtitle.text = "🌅 Morning Block Active"
                tvGreetingSubtitle.setTextColor(getColor(R.color.neon_yellow))
            }
            isInNightBlock -> {
                tvGreetingSubtitle.text = "🌙 Night Block Active"
                tvGreetingSubtitle.setTextColor(getColor(R.color.neon_purple))
            }
            isWeekend -> {
                val stats = pointsManager.getStatistics()
                val available = stats["weekend_minutes"] as? Int ?: 0
                val used = prefs.getInt("weekend_minutes_used_today", 0)
                val remaining = (available - used).coerceAtLeast(0)
                tvGreetingSubtitle.text = "🎉 Weekend Time! ($remaining min left)"
                tvGreetingSubtitle.setTextColor(getColor(R.color.neon_green))
            }
            else -> {
                tvGreetingSubtitle.text = "Unscroll Your Mind."
                tvGreetingSubtitle.setTextColor(getColor(R.color.text_secondary))
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