package com.example.scrollin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class AddTaskActivity : AppCompatActivity() {

    private lateinit var etTaskTitle: EditText
    private lateinit var etTaskPoints: EditText
    private lateinit var btnSaveTask: Button
    private lateinit var pointsManager: PointsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        pointsManager = PointsManager(this)

        etTaskTitle = findViewById(R.id.etTaskTitle)
        etTaskPoints = findViewById(R.id.etTaskPoints)
        btnSaveTask = findViewById(R.id.btnSaveTask)

        btnSaveTask.setOnClickListener {
            val title = etTaskTitle.text.toString()
            val points = etTaskPoints.text.toString().toIntOrNull()

            if (title.isNotEmpty() && points != null) {
                val newGoal = JourneyGoal(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    points = points,
                    isCompleted = false,
                    type = GoalType.GENERAL
                )
                pointsManager.addGoal(newGoal)
                finish()
            } else {
                Toast.makeText(this, "Please enter a valid title and points", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
