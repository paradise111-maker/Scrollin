package com.example.scrollin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    private var tvPoints: TextView? = null
    private var tvEarnedTime: TextView? = null
    private var tvStatus: TextView? = null
    private var progressBar: ProgressBar? = null
    private var btnMorningActivity: Button? = null
    private var btnNightActivity: Button? = null
    private var btnSettings: Button? = null
    private lateinit var pointsManager: PointsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)

            // Initialize points manager
            pointsManager = PointsManager(this)

            // Initialize views with null safety
            tvPoints = findViewById(R.id.tvPoints)
            tvEarnedTime = findViewById(R.id.tvEarnedTime)
            tvStatus = findViewById(R.id.tvStatus)
            progressBar = findViewById(R.id.progressBar)
            btnMorningActivity = findViewById(R.id.btnMorningActivity)
            btnNightActivity = findViewById(R.id.btnNightActivity)
            btnSettings = findViewById(R.id.btnSettings)

            // Update UI
            updateUI()

            // Set up button listeners
            btnMorningActivity?.setOnClickListener {
                startActivitySelection("morning")
            }

            btnNightActivity?.setOnClickListener {
                startActivitySelection("night")
            }

            btnSettings?.setOnClickListener {
                startActivity(Intent(this, SettingsActivity::class.java))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Show error to user
            tvStatus?.text = "Error: ${e.message}"
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            updateUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUI() {
        try {
            val points = pointsManager.getPoints()
            val earnedMinutes = pointsManager.getEarnedMinutes()

            tvPoints?.text = "Points: $points"
            tvEarnedTime?.text = "Earned Time: $earnedMinutes minutes"

            // Update progress bar (max 120 minutes = 2 hours)
            progressBar?.max = 120
            progressBar?.progress = earnedMinutes.coerceAtMost(120)

            // Check current block status
            updateBlockStatus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBlockStatus() {
        try {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
            val wakeHour = prefs.getInt("wake_hour", 7)

            val morningBlockEnd = (wakeHour + 3) % 24
            val isInMorningBlock = hour in wakeHour until morningBlockEnd
            val isInNightBlock = hour in 19..23

            when {
                isInMorningBlock -> {
                    tvStatus?.text = "🌅 Morning Block Active\nChoose a healthy activity!"
                    btnMorningActivity?.isEnabled = true
                    btnNightActivity?.isEnabled = false
                }
                isInNightBlock -> {
                    tvStatus?.text = "🌙 Night Block Active\nTime to wind down productively!"
                    btnMorningActivity?.isEnabled = false
                    btnNightActivity?.isEnabled = true
                }
                else -> {
                    tvStatus?.text = "✅ Free Time\nNo blocks active"
                    btnMorningActivity?.isEnabled = false
                    btnNightActivity?.isEnabled = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tvStatus?.text = "Status: Ready"
        }
    }

    private fun startActivitySelection(timeOfDay: String) {
        try {
            val intent = Intent(this, ActivitySelectionActivity::class.java)
            intent.putExtra("TIME_OF_DAY", timeOfDay)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}