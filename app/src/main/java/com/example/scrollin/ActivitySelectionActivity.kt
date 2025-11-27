package com.example.scrollin

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class ActivitySelectionActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var gridActivities: GridLayout
    private lateinit var tvTimer: TextView
    private lateinit var btnComplete: Button
    private lateinit var pointsManager: PointsManager

    private var selectedActivityPoints: Int = 0
    private var timer: CountDownTimer? = null
    private var isActivityRunning = false
    private var isActivityCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selection)

        pointsManager = PointsManager(this)
        val activityType = intent.getStringExtra("ACTIVITY_TYPE") ?: determineActivityType()

        // Initialize views from XML
        tvTitle = findViewById(R.id.tvTitle)
        gridActivities = findViewById(R.id.gridActivities)
        tvTimer = findViewById(R.id.tvTimer)
        btnComplete = findViewById(R.id.btnComplete)

        setupActivities(activityType)

        btnComplete.setOnClickListener {
            if (isActivityCompleted) {
                completeActivity()
            } else {
                Toast.makeText(this, "Please wait for the activity to complete!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun determineActivityType(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "MORNING"
            in 19..23 -> "NIGHT"
            else -> "ANYTIME"
        }
    }

    private fun setupActivities(type: String) {
        gridActivities.removeAllViews() // Clear previous activities

        when (type) {
            "MORNING" -> {
                tvTitle.text = "🌅 Morning Activities"
                addActivityToGrid("💪", "Push-ups", "10 reps | 10 pts", "PUSHUPS", 10, true)
                addActivityToGrid("🦵", "Squats", "15 reps | 10 pts", "SQUATS", 15, true)
                addActivityToGrid("🏃", "Jumping Jacks", "20 reps | 15 pts", "JUMPING_JACKS", 20, true)
                addActivityToGrid("🧘", "Yoga", "5 minutes | 10 pts", "YOGA", 0, false) // Camera optional for Yoga
                addActivityToGrid("✅", "Complete a Task", "Earn 5 pts", "TIMER", 5, false)
            }
            "NIGHT" -> {
                tvTitle.text = "🌙 Night Activities"
                addActivityToGrid("🧘‍♂️", "Meditation", "5-30 min | 10-40 pts", "MEDITATION", 0, false)
                addActivityToGrid("📚", "Reading", "10 minutes | 15 pts", "TIMER", 15, false)
                addActivityToGrid("📝", "Journaling", "5 minutes | 10 pts", "TIMER", 10, false)
                addActivityToGrid("👥", "Socialize", "Call a friend | 20 pts", "TIMER", 20, false)
            }
            else -> {
                tvTitle.text = "✨ Anytime Activities"
                 addActivityToGrid("🧘‍♂️", "Meditation", "5-30 min | 10-40 pts", "MEDITATION", 0, false)
                addActivityToGrid("💪", "Quick Exercise", "5 reps | 5 pts", "PUSHUPS", 5, true)
                addActivityToGrid("📚", "Reading", "10 minutes | 15 pts", "TIMER", 15, false)
            }
        }
    }

    private fun addActivityToGrid(icon: String, title: String, subtitle: String, type: String, target: Int, requiresCamera: Boolean) {
        val inflater = LayoutInflater.from(this)
        val gridItem = inflater.inflate(R.layout.item_activity_grid, gridActivities, false) as LinearLayout

        val tvIcon = gridItem.findViewById<TextView>(R.id.tvActivityIcon)
        val tvTitle = gridItem.findViewById<TextView>(R.id.tvActivityTitle)
        val tvSubtitle = gridItem.findViewById<TextView>(R.id.tvActivitySubtitle)

        tvIcon.text = icon
        tvTitle.text = title
        tvSubtitle.text = subtitle

        val points = subtitle.substringAfterLast("|").filter { it.isDigit() }.toIntOrNull() ?: 0

        gridItem.setOnClickListener {
            startActivityAction(title, points, requiresCamera, type, target)
        }

        val params = GridLayout.LayoutParams().apply {
            width = 0
            height = GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(8, 8, 8, 8)
        }
        gridItem.layoutParams = params
        gridActivities.addView(gridItem)
    }

    private fun startActivityAction(title: String, points: Int, requiresCamera: Boolean, exercise: String, target: Int) {
        if (isActivityRunning) {
            Toast.makeText(this, "Complete current activity first", Toast.LENGTH_SHORT).show()
            return
        }

        if (exercise == "MEDITATION") {
            showMeditationDialog()
            return
        }

        selectedActivityPoints = points
        
        if (requiresCamera) {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("ACTIVITY_TYPE", exercise)
            intent.putExtra("TARGET_REPS", target)
            intent.putExtra("ACTIVITY_NAME", title)
            intent.putExtra("POINTS", points)
            startActivityForResult(intent, 100)
        } else {
            startTimerActivity(title, points)
        }
    }

    private fun showMeditationDialog() {
        val meditationOptions = arrayOf("5 minutes (10 points)", "10 minutes (20 points)", "15 minutes (30 points)", "30 minutes (40 points)")
        val durations = arrayOf(300L, 600L, 900L, 1800L)
        val points = arrayOf(10, 20, 30, 40)

        AlertDialog.Builder(this)
            .setTitle("Choose Meditation Time")
            .setItems(meditationOptions) { _, which ->
                startTimerActivity("Meditation", points[which], durations[which])
            }
            .show()
    }

    private fun startTimerActivity(title: String, points: Int, duration: Long? = null) {
        isActivityRunning = true
        isActivityCompleted = false

        val durationSeconds = duration ?: when {
            title.contains("3 minutes") -> 180L
            title.contains("5 minutes") -> 300L
            title.contains("10 minutes") -> 600L
            else -> 120L // Default to 2 minutes
        }
        
        selectedActivityPoints = points

        tvTimer.text = "Activity: $title\nTime remaining: ${formatTime(durationSeconds)}"
        btnComplete.isEnabled = false
        btnComplete.alpha = 0.5f

        for (i in 0 until gridActivities.childCount) {
            (gridActivities.getChildAt(i) as? LinearLayout)?.getChildAt(0)?.alpha = 0.5f
        }

        timer = object : CountDownTimer(durationSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvTimer.text = "Activity: $title\nTime: ${formatTime(millisUntilFinished / 1000)}"
            }

            override fun onFinish() {
                isActivityCompleted = true
                tvTimer.text = "✅ Activity Complete!\n\nYou earned $selectedActivityPoints points!"
                btnComplete.isEnabled = true
                btnComplete.alpha = 1.0f
                Toast.makeText(this@ActivitySelectionActivity, "🎉 Great job!", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", minutes, secs)
    }

    private fun completeActivity() {
        pointsManager.addPoints(selectedActivityPoints)
        Toast.makeText(this, "🎉 +$selectedActivityPoints points earned!", Toast.LENGTH_LONG).show()
        
        btnComplete.postDelayed({
            finish() 
        }, 1000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val points = data?.getIntExtra("POINTS_EARNED", 0) ?: 0
            pointsManager.addPoints(points)
            Toast.makeText(this, "🎉 +$points points earned!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}