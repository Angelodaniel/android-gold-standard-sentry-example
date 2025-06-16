package com.example.androidcustommetricsapp

import android.app.Application

class MyApp : Application() {
    companion object {
        var appStartTime: Long = 0
    }
    override fun onCreate() {
        appStartTime = System.currentTimeMillis()
        // Start span for app start to first screen interactive
        Tracer.startSpan("app.start_to_first_screen")
        super.onCreate()
    }
} 