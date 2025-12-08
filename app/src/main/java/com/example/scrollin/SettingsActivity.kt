package com.example.scrollin

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var sliderWakeTime: Slider
    private lateinit var tvWakeTimeValue: TextView
    private lateinit var switchMorningBlock: SwitchMaterial
    private lateinit var switchNightBlock: SwitchMaterial
    private lateinit var tvTotalPoints: TextView
    private lateinit var tvTotalMinutes: TextView
    private lateinit var tvCurrentStreak: TextView
    private lateinit var tvCompletedTasks: TextView
    private lateinit var btnSave: Button
    private lateinit var btnAccessibility: Button
    private lateinit var btnResetStats: Button
    private lateinit var btnAbout: LinearLayout
    private lateinit var btnPrivacy: LinearLayout
    private lateinit var btnExportData: LinearLayout
    private lateinit var pointsManager: PointsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_redesigned)

        pointsManager = PointsManager(this)

        // Initialize views
        etName = findViewById(R.id.etName)
        sliderWakeTime = findViewById(R.id.sliderWakeTime)
        tvWakeTimeValue = findViewById(R.id.tvWakeTimeValue)
        switchMorningBlock = findViewById(R.id.switchMorningBlock)
        switchNightBlock = findViewById(R.id.switchNightBlock)
        tvTotalPoints = findViewById(R.id.tvTotalPoints)
        tvTotalMinutes = findViewById(R.id.tvTotalMinutes)
        tvCurrentStreak = findViewById(R.id.tvCurrentStreak)
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks)
        btnSave = findViewById(R.id.btnSave)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnResetStats = findViewById(R.id.btnResetStats)
        btnAbout = findViewById(R.id.btnAbout)
        btnPrivacy = findViewById(R.id.btnPrivacy)
        btnExportData = findViewById(R.id.btnExportData)

        loadSettings()
        updateStatsCards()
        setupSlider()

        btnSave.setOnClickListener { saveSettings() }
        btnAccessibility.setOnClickListener { openAccessibilitySettings() }
        btnResetStats.setOnClickListener { showResetConfirmation() }
        btnAbout.setOnClickListener { showAboutDialog() }
        btnPrivacy.setOnClickListener { showPrivacyInfo() }
        btnExportData.setOnClickListener { exportUserData() }
        
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
    }

    private fun setupSlider() {
        sliderWakeTime.addOnChangeListener { _, value, _ ->
            val hour = value.toInt()
            tvWakeTimeValue.text = formatHour(hour)
        }
    }

    private fun formatHour(hour: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$displayHour:00 $amPm"
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
        etName.setText(prefs.getString("user_name", ""))
        val wakeHour = prefs.getInt("wake_hour", 7)
        sliderWakeTime.value = wakeHour.toFloat()
        tvWakeTimeValue.text = formatHour(wakeHour)
        switchMorningBlock.isChecked = prefs.getBoolean("morning_block_enabled", true)
        switchNightBlock.isChecked = prefs.getBoolean("night_block_enabled", true)
    }

    private fun saveSettings() {
        val name = etName.text.toString()
        val wakeHour = sliderWakeTime.value.toInt()

        val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_name", name)
            putInt("wake_hour", wakeHour)
            putBoolean("morning_block_enabled", switchMorningBlock.isChecked)
            putBoolean("night_block_enabled", switchNightBlock.isChecked)
            apply()
        }

        Toast.makeText(this, "✅ Settings saved successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatsCards() {
        val stats = pointsManager.getStatistics()
        tvTotalPoints.text = "${stats["total_points"]} pts"
        
        val totalMinutes = (stats["total_minutes"] as? Int) ?: 0
        tvTotalMinutes.text = "${totalMinutes / 60}h ${totalMinutes % 60}m"
        
        tvCurrentStreak.text = "${stats["current_streak"]} days"
        tvCompletedTasks.text = "${stats["tasks_completed"]} tasks"
    }

    private fun showResetConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Reset All Data?")
            .setMessage("This will permanently delete all your progress, points, and statistics. This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                pointsManager.resetAllProgress()
                updateStatsCards()
                Toast.makeText(this, "All data has been reset", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(
            this,
            "Enable 'Scrollin' in accessibility services to block distracting apps",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About Scrollin")
            .setMessage("""
                Scrollin v1.0
                
                Fight doomscrolling by earning back your time through productive activities.
                
                • Complete exercises, meditation, and tasks
                • Earn points and unlock weekend time
                • Track your progress and build streaks
                • Block distracting apps during focus times
                
                Made with ❤️ to help you reclaim your time
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showPrivacyInfo() {
        AlertDialog.Builder(this)
            .setTitle("Privacy & Data")
            .setMessage("""
                Your Privacy Matters
                
                • All data is stored locally on your device
                • No personal information is collected or shared
                • Accessibility service only monitors app launches
                • You can export or delete your data anytime
                
                We respect your privacy and never track or sell your data.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun exportUserData() {
        val stats = pointsManager.getStatistics()
        val goals = pointsManager.getGoals()
        
        val data = """
            SCROLLIN DATA EXPORT
            ====================
            
            Stats:
            - Total Points: ${stats["total_points"]}
            - Current Streak: ${stats["current_streak"]} days
            - Tasks Completed: ${stats["tasks_completed"]}
            - Weekend Minutes: ${stats["weekend_minutes"]}
            
            Active Goals: ${goals.count { !it.isCompleted }}
            Completed Goals: ${goals.count { it.isCompleted }}
            
            Export Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
        """.trimIndent()
        
        // Create share intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, data)
            putExtra(Intent.EXTRA_SUBJECT, "Scrollin Data Export")
        }
        
        startActivity(Intent.createChooser(shareIntent, "Export Data"))
    }
}
