package com.example.scrollin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("ScrollinPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("weekend_minutes_used_today", 0).apply()
        
        // Reset other daily stats if needed
    }
}