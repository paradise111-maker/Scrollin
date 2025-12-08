package com.example.scrollin

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import java.util.*

class DoomscrollAccessibilityService : AccessibilityService() {

    private var overlayView: android.view.View? = null
    private var weekendTimerView: android.view.View? = null
    private var windowManager: WindowManager? = null
    private lateinit var pointsManager: PointsManager

    // List of blocked apps (package names)
    private val blockedApps = setOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.snapchat.android",
        "com.zhiliaoapp.musically" // TikTok
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        pointsManager = PointsManager(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()

            if (packageName != null && blockedApps.contains(packageName)) {
                if (shouldBlockApp()) {
                    showBlockOverlay(packageName)
                } else {
                    removeBlockOverlay()
                }
            }
        }
    }

    private fun shouldBlockApp(): Boolean {
        val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

        val morningBlockEnabled = prefs.getBoolean("morning_block_enabled", true)
        val nightBlockEnabled = prefs.getBoolean("night_block_enabled", true)
        val wakeHour = prefs.getInt("wake_hour", 7)

        val morningBlockEnd = (wakeHour + 3) % 24
        val isInMorningBlock = hour in wakeHour until morningBlockEnd
        val isInNightBlock = hour in 19..23

        // Weekend time logic
        if (isWeekend) {
            val usedMinutes = prefs.getInt("weekend_minutes_used_today", 0)
            val stats = pointsManager.getStatistics()
            val availableMinutes = stats["weekend_minutes"] as? Int ?: 0
            
            return usedMinutes >= availableMinutes
        }

        return ((morningBlockEnabled && isInMorningBlock) ||
                (nightBlockEnabled && isInNightBlock))
    }

    private fun showBlockOverlay(appPackage: String) {
        // Remove existing overlay if present
        removeBlockOverlay()

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.block_overlay, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        // Update overlay text
        val tvMessage = overlayView?.findViewById<TextView>(R.id.tvBlockMessage)
        val tvEarnedTime = overlayView?.findViewById<TextView>(R.id.tvEarnedTime)
        val btnGoBack = overlayView?.findViewById<Button>(R.id.btnGoBack)
        val btnEarnTime = overlayView?.findViewById<Button>(R.id.btnEarnTime)
        val btnUseWeekendTime = overlayView?.findViewById<Button>(R.id.btnUseWeekendTime)

        val appName = getAppName(appPackage)
        val stats = pointsManager.getStatistics()
        val earnedMinutes = stats["weekend_minutes"] as? Int ?: 0

        tvMessage?.text = "🚫 $appName is blocked during this time"
        tvEarnedTime?.text = "You have $earnedMinutes minutes of earned time"

        val isWeekend = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        if(isWeekend && earnedMinutes > 0) {
            btnUseWeekendTime?.visibility = View.VISIBLE
        }

        btnGoBack?.setOnClickListener {
            removeBlockOverlay()
            // Go to home screen
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
        }

        btnEarnTime?.setOnClickListener {
            removeBlockOverlay()
            // Open main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
        
        btnUseWeekendTime?.setOnClickListener {
            removeBlockOverlay()
            showWeekendTimerOverlay(appName)
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeBlockOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }
    
    private fun showWeekendTimerOverlay(appName: String) {
        removeWeekendTimerOverlay()

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        weekendTimerView = inflater.inflate(R.layout.weekend_timer_overlay, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val tvAppName = weekendTimerView?.findViewById<TextView>(R.id.tvAppName)
        val tvTimerDisplay = weekendTimerView?.findViewById<TextView>(R.id.tvTimerDisplay)
        val btnStopSession = weekendTimerView?.findViewById<Button>(R.id.btnStopSession)
        val progressTimeRemaining = weekendTimerView?.findViewById<ProgressBar>(R.id.progressTimeRemaining)
        
        tvAppName?.text = "Using $appName"
        
        startWeekendTimer(tvTimerDisplay, progressTimeRemaining)

        btnStopSession?.setOnClickListener {
            removeWeekendTimerOverlay()
            showBlockOverlay(appName)
        }

        try {
            windowManager?.addView(weekendTimerView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun removeWeekendTimerOverlay() {
        endWeekendTimer()
        weekendTimerView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            weekendTimerView = null
        }
    }

    private fun startWeekendTimer(timerDisplay: TextView?, progressBar: ProgressBar?) {
        val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
        val startTime = System.currentTimeMillis()
        prefs.edit().putLong("weekend_session_start", startTime).apply()
        
        val stats = pointsManager.getStatistics()
        val availableMinutes = stats["weekend_minutes"] as? Int ?: 0
        val usedMinutes = prefs.getInt("weekend_minutes_used_today", 0)
        val remainingMillis = ((availableMinutes - usedMinutes) * 60 * 1000).toLong()

        object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remainingSeconds = millisUntilFinished / 1000
                val hours = remainingSeconds / 3600
                val minutes = (remainingSeconds % 3600) / 60
                val secs = remainingSeconds % 60
                timerDisplay?.text = String.format("⏰ %02d:%02d:%02d left", hours, minutes, secs)
                progressBar?.progress = (millisUntilFinished * 100 / remainingMillis).toInt()
            }

            override fun onFinish() {
                removeWeekendTimerOverlay()
                showBlockOverlay("")
            }
        }.start()
    }

    private fun endWeekendTimer() {
        val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
        val startTime = prefs.getLong("weekend_session_start", 0)
        if (startTime > 0) {
            val minutesUsed = ((System.currentTimeMillis() - startTime) / 60000).toInt()
            val totalUsed = prefs.getInt("weekend_minutes_used_today", 0)
            prefs.edit().putInt("weekend_minutes_used_today", totalUsed + minutesUsed).apply()
        }
    }

    private fun getAppName(packageName: String): String {
        return when (packageName) {
            "com.instagram.android" -> "Instagram"
            "com.facebook.katana" -> "Facebook"
            "com.twitter.android" -> "Twitter"
            "com.snapchat.android" -> "Snapchat"
            "com.zhiliaoapp.musically" -> "TikTok"
            else -> "This app"
        }
    }

    override fun onInterrupt() {
        removeBlockOverlay()
        removeWeekendTimerOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBlockOverlay()
        removeWeekendTimerOverlay()
    }
}
