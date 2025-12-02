package com.example.scrollin

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etWakeTime: EditText
    private lateinit var switchMorningBlock: Switch
    private lateinit var switchNightBlock: Switch
    private lateinit var tvStats: TextView
    private lateinit var btnSave: Button
    private lateinit var btnAccessibility: Button
    private lateinit var btnResetStats: Button
    private lateinit var pointsManager: PointsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        pointsManager = PointsManager(this)

        // Initialize views from XML
        etName = findViewById(R.id.etName)
        etWakeTime = findViewById(R.id.etWakeTime)
        switchMorningBlock = findViewById(R.id.switchMorningBlock)
        switchNightBlock = findViewById(R.id.switchNightBlock)
        tvStats = findViewById(R.id.tvStats)
        btnSave = findViewById(R.id.btnSave)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnResetStats = findViewById(R.id.btnResetStats)

        loadSettings()
        updateStats()

        btnSave.setOnClickListener { saveSettings() }
        btnAccessibility.setOnClickListener { openAccessibilitySettings() }
        btnResetStats.setOnClickListener { resetStatistics() }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
        etName.setText(prefs.getString("user_name", ""))
        etWakeTime.setText(prefs.getInt("wake_hour", 7).toString())
        switchMorningBlock.isChecked = prefs.getBoolean("morning_block_enabled", true)
        switchNightBlock.isChecked = prefs.getBoolean("night_block_enabled", true)
    }

    private fun saveSettings() {
        val name = etName.text.toString()
        val wakeHour = etWakeTime.text.toString().toIntOrNull() ?: 7

        if (wakeHour !in 0..23) {
            Toast.makeText(this, "Please enter a valid hour (0-23)", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_name", name)
            putInt("wake_hour", wakeHour)
            putBoolean("morning_block_enabled", switchMorningBlock.isChecked)
            putBoolean("night_block_enabled", switchNightBlock.isChecked)
            apply()
        }

        Toast.makeText(this, "✅ Settings saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateStats() {
        val stats = pointsManager.getStatistics()
        val totalPoints = stats["total_points"] as? Int ?: 0
        val earnedToday = stats["earned_today"] as? Int ?: 0 // This key doesn't exist, will be 0
        val usedToday = stats["used_today"] as? Int ?: 0 // This key doesn't exist, will be 0

        tvStats.text = """
            📊 Your Statistics
            
            Total Points: $totalPoints
            Minutes Earned Today: $earnedToday
            Minutes Used Today: $usedToday
        """.trimIndent()
    }

    private fun resetStatistics() {
        pointsManager.resetAllProgress()
        updateStats()
        Toast.makeText(this, "Statistics reset!", Toast.LENGTH_SHORT).show()
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(
            this,
            "Please enable 'Scrollin' in the accessibility services list",
            Toast.LENGTH_LONG
        ).show()
    }
}
