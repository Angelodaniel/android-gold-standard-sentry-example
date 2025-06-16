package com.example.androidcustommetricsapp

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.Instant
import io.sentry.ITransaction
import io.sentry.ISpan
import io.sentry.Sentry
import io.sentry.SentryInstantDate

class AutoTTIDTTFDWithNTSMeasurementActivity : AppCompatActivity() {
    private val client by lazy { OkHttpClient() }
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var screenOpenedTime: Long = 0
    private var screenTTISpan: ISpan? = null
    private var tapTime: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tapTime = intent.getLongExtra("tap_time", -1)
        val ntsSpanOp = intent.getStringExtra("nts_span_op") ?: "navigation.to.AutoTTIDTTFDWithNTS"
        val transaction = Sentry.getSpan() as? ITransaction
        screenOpenedTime = System.currentTimeMillis()
        // Finish the NTS span started in HomeActivity
        Tracer.stopSpan(ntsSpanOp)
        // Optionally, create a new NTS span as a child of the current transaction for trace continuity
        if (transaction != null && tapTime > 0) {
            val startTimestamp = SentryInstantDate(Instant.ofEpochMilli(tapTime))
            val ntsSpan = transaction.startChild(
                "navigation.to.screen",
                "Navigation to Screen",
                startTimestamp
            )
            ntsSpan.setData("screen_origin", "HomeActivity")
            ntsSpan.setData("screen_destination", "AutoTTIDTTFDWithNTSMeasurementActivity")
            // Finish the span at the current time (custom timestamp not supported)
            ntsSpan.finish()
        }
        // Start TTI span for this screen
        if (transaction != null && tapTime > 0) {
            val fakeStartTime = tapTime - 300
            val startTimestamp = SentryInstantDate(Instant.ofEpochMilli(fakeStartTime))
            screenTTISpan = transaction.startChild(
                "ui.screen_time_to_interactive",
                "Screen Time to Interactive",
                startTimestamp
            )
            // NTS: from tap to screen opened
            val nts = screenOpenedTime - tapTime
            screenTTISpan?.setData("nts_ms", nts)
        }

        setContentView(R.layout.activity_screen_v)

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val statusText = findViewById<TextView>(R.id.statusText)

        progressBar.visibility = View.VISIBLE
        statusText.text = "Loading..."

        scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    statusText.text = "Loading network data..."
                }
                withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://jsonplaceholder.typicode.com/posts/1")
                        .build()
                    client.newCall(request).execute().use { response ->
                        response.body?.string()
                    }
                }

                withContext(Dispatchers.Main) {
                    statusText.text = "Loading file data..."
                }
                withContext(Dispatchers.IO) {
                    val file = File(filesDir, "manual_demo.txt")
                    file.writeText("Hello, Sentry!")
                    file.readText()
                }

                withContext(Dispatchers.Main) {
                    statusText.text = "Loading rich content..."
                }
                delay(1000) // Simulate image loading

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    statusText.text = "All content loaded!"
                    Sentry.reportFullyDisplayed()
                    // TTI: from screen opened to fully interactive
                    val fullyInteractiveTime = System.currentTimeMillis()
                    val tti = fullyInteractiveTime - screenOpenedTime
                    screenTTISpan?.setData("tti_ms", tti)
                    screenTTISpan?.finish() // Finish the custom span after TTFD
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    statusText.text = "Error occurred"
                    Toast.makeText(
                        this@AutoTTIDTTFDWithNTSMeasurementActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        scope.coroutineContext.cancel()
        super.onDestroy()
    }
} 