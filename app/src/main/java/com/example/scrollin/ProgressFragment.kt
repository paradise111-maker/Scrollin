package com.example.scrollin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter

class ProgressFragment : Fragment() {
    private lateinit var pointsManager: PointsManager
    private lateinit var levelManager: UserLevelManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pointsManager = PointsManager(requireContext())
        levelManager = UserLevelManager(requireContext())

        setupWeeklyChart(view)
        setupCategoryChart(view)
        updateProgressStats(view)
    }

    private fun setupWeeklyChart(view: View) {
        val lineChart = view.findViewById<LineChart>(R.id.lineChartWeekly)
        val pointsData = pointsManager.getLast7DaysPoints()

        val entries = ArrayList<Entry>()
        for ((index, points) in pointsData.withIndex()) {
            entries.add(Entry(index.toFloat(), points.toFloat()))
        }

        val dataSet = LineDataSet(entries, "Daily Points").apply {
            color = ContextCompat.getColor(requireContext(), R.color.neon_green)
            lineWidth = 3f
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.neon_green)
            fillAlpha = 50
            setDrawCircles(true)
            circleRadius = 5f
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.neon_green))
            mode = LineDataSet.Mode.CUBIC_BEZIER
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }

        lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.textColor = Color.WHITE
            axisLeft.textColor = Color.WHITE
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.GRAY
                valueFormatter = DayAxisValueFormatter()
            }
            animateX(1000)
            invalidate()
        }
    }

    private fun setupCategoryChart(view: View) {
        val barChart = view.findViewById<BarChart>(R.id.barChartCategories)
        val stats = pointsManager.getStatistics()

        // Calculate points by category (you'''ll need to add this to PointsManager)
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, 45f)) // Physical
        entries.add(BarEntry(1f, 30f)) // Mental
        entries.add(BarEntry(2f, 60f)) // Productivity

        val dataSet = BarDataSet(entries, "Points by Category").apply {
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.neon_green),
                ContextCompat.getColor(requireContext(), R.color.neon_purple),
                ContextCompat.getColor(requireContext(), R.color.neon_blue)
            )
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }

        barChart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            legend.textColor = Color.WHITE
            axisLeft.textColor = Color.WHITE
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.WHITE
                valueFormatter = CategoryAxisValueFormatter()
                granularity = 1f
            }
            animateY(1000)
            invalidate()
        }
    }

    private fun updateProgressStats(view: View) {
        val stats = pointsManager.getStatistics()
        
        view.findViewById<TextView>(R.id.tvTotalPoints).text = 
            "${stats["total_points"]} pts"
        
        view.findViewById<TextView>(R.id.tvTasksCompleted).text = 
            "${stats["tasks_completed"]} tasks"
        
        view.findViewById<TextView>(R.id.tvAveragePoints).text = 
            "${stats["weekly_points"].toString().toIntOrNull()?.div(7) ?: 0} pts/day"
        
        // Completion rate calculation
        val goals = pointsManager.getGoals()
        val completedGoals = goals.count { it.isCompleted }
        val totalGoals = goals.size
        val completionRate = if (totalGoals > 0) {
            (completedGoals * 100) / totalGoals
        } else 0
        
        view.findViewById<TextView>(R.id.tvCompletionRate).text = 
            "$completionRate%"
    }

    inner class DayAxisValueFormatter : ValueFormatter() {
        private val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
            return days.getOrNull(value.toInt()) ?: ""
        }
    }

    inner class CategoryAxisValueFormatter : ValueFormatter() {
        private val categories = arrayOf("Physical", "Mental", "Productivity")
        override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
            return categories.getOrNull(value.toInt()) ?: ""
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { updateProgressStats(it) }
    }
}