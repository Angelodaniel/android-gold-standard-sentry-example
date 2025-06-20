package com.example.androidcustommetricsapp

import android.app.Application

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(Tracer)
    }
} 