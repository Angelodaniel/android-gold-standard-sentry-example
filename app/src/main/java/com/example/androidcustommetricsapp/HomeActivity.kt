package com.example.androidcustommetricsapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.coroutineScope

/**
 * HomeActivity
 *
 * Main entry screen. Demonstrates navigation instrumentation for Sentry.
 * Each button click starts a navigation span/transaction for Sentry performance tracing.
 * See Tracer and BaseInstrumentedActivity for details on how navigation and screen performance are instrumented.
 */
class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        findViewById<Button>(R.id.btnAutoNTS).apply {
            text = "Auto TTID/TTFD + NTS (span, tap-to-open)"
            setOnClickListener {
                this@HomeActivity.launchTraced(AutoTTIDTTFDWithNTSMeasurementActivity::class.java) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Simulate 1 second work for launching next screen
                        putExtra("heavy_extra", true)
                    }, 1000)
                }
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // Simulate 1 second work for fully drawn
            // Mark Home screen as fully displayed for Sentry TTFD/cold start
            Tracer.reportFullyDrawn(this)
        }, 500)
    }
}
