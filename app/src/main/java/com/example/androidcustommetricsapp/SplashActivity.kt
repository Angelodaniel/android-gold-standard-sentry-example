package com.example.androidcustommetricsapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import io.sentry.Sentry

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Start a manual span for time to first screen
        val firstScreenSpan = Tracer.startSpan("app.time_to_first_screen")

        Handler(Looper.getMainLooper()).postDelayed({
            firstScreenSpan.finish()
            // Start a span for first screen to home
            Tracer.startSpan("first_screen_to_home")
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }, 1000)
    }
}
