package com.example.androidcustommetricsapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Simulate app initialization
        Handler(Looper.getMainLooper()).postDelayed({
            // Stop app start to first screen span, start first screen to home span
            Tracer.stopSpan("app.start_to_first_screen")
            Tracer.startSpan("first_screen_to_home")
            val firstScreenReadyTime = System.currentTimeMillis()
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("app_start_time", MyApp.appStartTime)
            intent.putExtra("first_screen_ready_time", firstScreenReadyTime)
            startActivity(intent)
            finish()
        }, 1000) // Simulate 1 second initialization
    }
}
