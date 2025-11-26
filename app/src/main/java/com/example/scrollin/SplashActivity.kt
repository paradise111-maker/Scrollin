package com.example.scrollin

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY = 2000L // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val tvSplashTitle = findViewById<TextView>(R.id.tvSplashTitle)

        // Fade-in animation
        val fadeIn = ObjectAnimator.ofFloat(tvSplashTitle, "alpha", 0f, 1f)
        fadeIn.duration = 1500
        fadeIn.interpolator = AccelerateDecelerateInterpolator()
        fadeIn.start()

        // Delayed transition to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Finish this activity so user can't go back to it
        }, SPLASH_DELAY)
    }
}