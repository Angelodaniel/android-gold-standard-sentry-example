package com.example.androidcustommetricsapp

import android.app.Application
import android.content.Context
import android.os.SystemClock
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.TransactionOptions
import io.sentry.ISpan

class MyApp : Application() {
    companion object {
        var appStartTransaction: ISpan? = null
        var firstScreenToHomeSpan: ISpan? = null
        var appStartTime: Long = 0
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Start the app.start_to_interactive transaction as early as possible
        val options = TransactionOptions().apply {
            isBindToScope = true  // Bind to scope so all spans become children
        }
        appStartTransaction = Sentry.startTransaction(
            "app.start_to_interactive",
            "App Start to Interactive",
            options
        )
        appStartTime = SystemClock.uptimeMillis()
    }

    override fun onCreate() {
        super.onCreate()
        // Disable activity auto-instrumentation
        io.sentry.Sentry.configureScope { scope ->
            scope.setTag("auto_instrument_activities", "false")
        }
        // Transaction is already started in attachBaseContext
    }
} 