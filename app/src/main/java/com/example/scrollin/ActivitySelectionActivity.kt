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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class ActivitySelectionActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var gridActivities: GridLayout
    private lateinit var tvTimer: TextView
    private lateinit var btnComplete: Button
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var pointsManager: PointsManager

    private var selectedActivityPoints: Int = 0
    private var selectedActivityCategory: Category = Category.GENERAL
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
        fabAddTask = findViewById(R.id.fabAddTask)

        setupActivities(activityType)

        btnComplete.setOnClickListener {
            if (isActivityCompleted) {
                completeActivity()
            } else {
                Toast.makeText(this, "Please wait for the activity to complete!", Toast.LENGTH_SHORT).show()
            }
        }

        fabAddTask.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
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
                addActivityToGrid("🧘‍♂️", "Meditation", "5-30 min | 10-40 pts", Category.MENTAL, 0, false)
                addActivityToGrid("💪", "Push-ups", "10 reps | 10 pts", Category.PHYSICAL, 10, true)
                addActivityToGrid("🦵", "Squats", "15 reps | 10 pts", Category.PHYSICAL, 15, true)
                addActivityToGrid("🏃", "Jumping Jacks", "20 reps | 15 pts", Category.PHYSICAL, 20, true)
                addActivityToGrid("🧘", "Yoga", "20-30 min | 25-35 pts", Category.PHYSICAL, 0, false)
                addActivityToGrid("✅", "Complete a Task", "30 sec | 10 pts", Category.PRODUCTIVITY, 0, false)
            }
            "NIGHT" -> {
                tvTitle.text = "🌙 Night Activities"
                addActivityToGrid("🧘‍♂️", "Meditation", "5-30 min | 10-40 pts", Category.MENTAL, 0, false)
                addActivityToGrid("📚", "Reading", "30-60 min | 20-40 pts", Category.PRODUCTIVITY, 0, false)
                addActivityToGrid("📝", "Journaling", "25-60 min | 20-40 pts", Category.MENTAL, 0, false)
                addActivityToGrid("👥", "Socialize", "30-60 min | 15-35 pts", Category.MENTAL, 0, false)
            }
            else -> {
                tvTitle.text = "✨ Anytime Activities"
                 addActivityToGrid("🧘‍♂️", "Meditation", "5-30 min | 10-40 pts", Category.MENTAL, 0, false)
                addActivityToGrid("💪", "Quick Exercise", "5 reps | 5 pts", Category.PHYSICAL, 5, true)
                addActivityToGrid("📚", "Reading", "10 minutes | 15 pts", Category.PRODUCTIVITY, 0, false)
            }
        }
    }

    private fun addActivityToGrid(icon: String, title: String, subtitle: String, category: Category, target: Int, requiresCamera: Boolean) {
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
            startActivityAction(title, points, requiresCamera, category, target)
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

    private fun startActivityAction(title: String, points: Int, requiresCamera: Boolean, category: Category, target: Int) {
        if (isActivityRunning) {
            Toast.makeText(this, "Complete current activity first", Toast.LENGTH_SHORT).show()
            return
        }

        this.selectedActivityCategory = category

        when (title) {
            "Complete a Task" -> startTimerActivity(title, 10, 30, category)
            "Meditation" -> showMeditationDialog(category)
            "Yoga" -> showYogaDialog(category)
            "Reading" -> showReadingDialog(category)
            "Journaling" -> showJournalingDialog(category)
            "Socialize" -> showSocializeDialog(category)
            else -> {
                selectedActivityPoints = points
                if (requiresCamera) {
                    val intent = Intent(this, CameraActivity::class.java)
                    intent.putExtra("ACTIVITY_TYPE", title)
                    intent.putExtra("TARGET_REPS", target)
                    intent.putExtra("ACTIVITY_NAME", title)
                    intent.putExtra("POINTS", points)
                    startActivityForResult(intent, 100)
                } else {
                    startTimerActivity(title, points, null, category)
                }
            }
        }
    }

    private fun showMeditationDialog(category: Category) {
        val options = arrayOf("5 minutes (10 points)", "10 minutes (20 points)", "15 minutes (30 points)", "30 minutes (40 points)")
        val durations = arrayOf(300L, 600L, 900L, 1800L)
        val points = arrayOf(10, 20, 30, 40)

        AlertDialog.Builder(this)
            .setTitle("Choose Meditation Time")
            .setItems(options) { _, which ->
                startTimerActivity("Meditation", points[which], durations[which], category)
            }
            .show()
    }

    private fun showYogaDialog(category: Category) {
        val options = arrayOf("20 minutes (25 points)", "30 minutes (35 points)")
        val durations = arrayOf(1200L, 1800L)
        val points = arrayOf(25, 35)

        AlertDialog.Builder(this)
            .setTitle("Choose Yoga Time")
            .setItems(options) { _, which ->
                startTimerActivity("Yoga", points[which], durations[which], category)
            }
            .show()
    }

    private fun showReadingDialog(category: Category) {
        val options = arrayOf("30 minutes (20 points)", "45 minutes (30 points)", "60 minutes (40 points)")
        val durations = arrayOf(1800L, 2700L, 3600L)
        val points = arrayOf(20, 30, 40)

        AlertDialog.Builder(this)
            .setTitle("Choose Reading Time")
            .setItems(options) { _, which ->
                startTimerActivity("Reading", points[which], durations[which], category)
            }
            .show()
    }

    private fun showJournalingDialog(category: Category) {
        val options = arrayOf("25 minutes (20 points)", "45 minutes (30 points)", "60 minutes (40 points)")
        val durations = arrayOf(1500L, 2700L, 3600L)
        val points = arrayOf(20, 30, 40)

        AlertDialog.Builder(this)
            .setTitle("Choose Journaling Time")
            .setItems(options) { _, which ->
                startTimerActivity("Journaling", points[which], durations[which], category)
            }
            .show()
    }

    private fun showSocializeDialog(category: Category) {
        val options = arrayOf("30 minutes (15 points)", "60 minutes (35 points)")
        val durations = arrayOf(1800L, 3600L)
        val points = arrayOf(15, 35)

        AlertDialog.Builder(this)
            .setTitle("Choose Socializing Time")
            .setItems(options) { _, which ->
                startTimerActivity("Socialize", points[which], durations[which], category)
            }
            .show()
    }

    private fun startTimerActivity(title: String, points: Int, duration: Long? = null, category: Category) {
        isActivityRunning = true
        isActivityCompleted = false

        val durationSeconds = duration ?: when {
            title.contains("3 minutes") -> 180L
            title.contains("5 minutes") -> 300L
            title.contains("10 minutes") -> 600L
            else -> 120L // Default to 2 minutes
        }
        
        selectedActivityPoints = points
        selectedActivityCategory = category

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
        pointsManager.addPoints(selectedActivityPoints, selectedActivityCategory)
        
        // Mark corresponding journey goal as complete
        val goals = pointsManager.getGoals()
        val matchingGoal = goals.find { goal ->
            !goal.isCompleted && 
            goal.category == selectedActivityCategory &&
            goal.estimatedTime != null
        }
        
        matchingGoal?.let { goal ->
            pointsManager.completeGoal(goal)
        }
        
        Toast.makeText(this, "🎉 +$selectedActivityPoints points earned!", Toast.LENGTH_LONG).show()
        
        btnComplete.postDelayed({
            finish() 
        }, 1000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val points = data?.getIntExtra("POINTS_EARNED", 0) ?: 0
            pointsManager.addPoints(points, selectedActivityCategory)
            Toast.makeText(this, "🎉 +$points points earned!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
