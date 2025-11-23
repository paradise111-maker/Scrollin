package com.example.scrollin

import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.content.Intent
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView
import java.util.*

class DoomscrollAccessibilityService : AccessibilityService() {

    private var overlayView: android.view.View? = null
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
                }
            }
        }
    }

    private fun shouldBlockApp(): Boolean {
        val prefs = getSharedPreferences("ScrollinPrefs", MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val morningBlockEnabled = prefs.getBoolean("morning_block_enabled", true)
        val nightBlockEnabled = prefs.getBoolean("night_block_enabled", true)
        val wakeHour = prefs.getInt("wake_hour", 7)

        val morningBlockEnd = (wakeHour + 3) % 24
        val isInMorningBlock = hour in wakeHour until morningBlockEnd
        val isInNightBlock = hour in 19..23

        // Check if user has earned minutes
        val hasEarnedTime = pointsManager.getEarnedMinutes() > 0

        return ((morningBlockEnabled && isInMorningBlock) ||
                (nightBlockEnabled && isInNightBlock)) && !hasEarnedTime
    }

    private fun showBlockOverlay(appPackage: String) {
        // Remove existing overlay if present
        removeOverlay()

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

        val appName = getAppName(appPackage)
        val earnedMinutes = pointsManager.getEarnedMinutes()

        tvMessage?.text = "🚫 $appName is blocked during this time"
        tvEarnedTime?.text = "You have $earnedMinutes minutes of earned time"

        btnGoBack?.setOnClickListener {
            removeOverlay()
            // Go to home screen
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
        }

        btnEarnTime?.setOnClickListener {
            removeOverlay()
            // Open main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
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
        removeOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }
}