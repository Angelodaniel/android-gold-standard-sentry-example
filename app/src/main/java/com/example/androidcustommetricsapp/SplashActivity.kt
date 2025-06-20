package com.example.androidcustommetricsapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import io.sentry.Sentry
import io.sentry.ISpan
import io.sentry.SpanStatus

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Simulate app initialization
        Handler(Looper.getMainLooper()).postDelayed({
            Tracer.reportFullyDrawn(this)

            startHomeActivity()
        }, 1000) // Simulate 1 second initialization
    }

    private fun startHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
