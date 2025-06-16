package com.example.androidcustommetricsapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.TransactionOptions
import io.sentry.ISpan

/**
 * HomeActivity
 *
 * Main entry screen. Demonstrates navigation instrumentation for Sentry.
 * Each button click starts a navigation span/transaction for Sentry performance tracing.
 * See Tracer and BaseInstrumentedActivity for details on how navigation and screen performance are instrumented.
 */
class HomeActivity : AppCompatActivity() {
    private var ntsSpan: ISpan? = null
    private var homeToInteractiveSpan: ISpan? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Finish the span from first screen to home
        Tracer.stopSpan("first_screen_to_home")
        // Start a manual span for home to interactive
        homeToInteractiveSpan = Tracer.startSpan("home_to_interactive")

        // Example: Custom Navigation Timing Span (NTS)
        val tapTimestamp = intent.getLongExtra("tap_timestamp", -1L)
        if (tapTimestamp > 0) {
            val now = System.currentTimeMillis()
            val durationMs = now - tapTimestamp
            val currentSpan = Sentry.getSpan()
            val ntsSpan = currentSpan?.startChild("navigation.tti", "Navigation Tap to Home TTI")
            ntsSpan?.setData("navigation_tti_ms", durationMs)
            currentSpan?.setData("navigation_tti_ms", durationMs)
            ntsSpan?.finish(SpanStatus.OK)
        }

        findViewById<Button>(R.id.btnAutoNTS).apply {
            text = "Screen Time to Interactive (NTS + TTFD)"
            setOnClickListener {
                // Start a custom transaction for the NTS/TTFD demo and bind to scope
                val options = TransactionOptions().apply { isBindToScope = true }
                val transaction = Sentry.startTransaction(
                    "screen.time_to_interactive",
                    "Screen Time To Interactive",
                    options
                )
                // Create and start the NTS span using Tracer
                ntsSpan = Tracer.startSpan("navigation.tti")
                startActivity(Intent(this@HomeActivity, AutoTTIDTTFDWithNTSMeasurementActivity::class.java))
            }
        }
        Sentry.reportFullyDisplayed()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Finish the NTS span when HomeActivity is destroyed
        ntsSpan?.finish(SpanStatus.OK)
        // Finish the home to interactive span
        homeToInteractiveSpan?.finish(SpanStatus.OK)
    }
}
