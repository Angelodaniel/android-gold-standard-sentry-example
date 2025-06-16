package com.example.androidcustommetricsapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.sentry.Sentry
import io.sentry.ITransaction
import io.sentry.SentryInstantDate
import java.time.Instant

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

        // Stop first screen to home span, start app start to interactive span
        val appStartTime = intent.getLongExtra("app_start_time", MyApp.appStartTime)
        Tracer.stopSpan("first_screen_to_home")
        val transaction = Sentry.getSpan() as? ITransaction
        if (transaction != null && appStartTime > 0) {
            val startTimestamp = SentryInstantDate(Instant.ofEpochMilli(appStartTime))
            val appStartToInteractiveSpan = transaction.startChild(
                "app.start_to_interactive",
                "App Start to Interactive",
                startTimestamp
            )
            // Finish the span at the current time (custom timestamp not supported)
            appStartToInteractiveSpan.finish()
        }

        findViewById<Button>(R.id.btnAutoNTS).apply {
            text = "Auto TTID/TTFD + NTS (span, tap-to-open)"
            setOnClickListener {
                val tapTime = System.currentTimeMillis()
                val ntsOp = "navigation.to.AutoTTIDTTFDWithNTS"
                val ntsSpan = Tracer.startSpan(ntsOp)
                ntsSpan.setData("screen_origin", "HomeActivity")
                ntsSpan.setData("screen_destination", "AutoTTIDTTFDWithNTSMeasurementActivity")
                ntsSpan.setData("tap_time", tapTime)
                val intent = Intent(this@HomeActivity, AutoTTIDTTFDWithNTSMeasurementActivity::class.java)
                intent.putExtra("nts_span_op", ntsOp)
                intent.putExtra("tap_time", tapTime)
                startActivity(intent)
            }
        }
        // Mark Home screen as fully displayed for Sentry TTFD/cold start
        Sentry.reportFullyDisplayed()
    }
}
