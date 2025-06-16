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
import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.SpanStatus
import com.example.androidcustommetricsapp.Tracer

class AutoTTIDTTFDWithNTSMeasurementActivity : AppCompatActivity() {
    private val client by lazy { OkHttpClient() }
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var ntsSpan: ITransaction? = null
    private var ttiSpan: ITransaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_v)

        // Start a custom transaction for screen.time_to_interactive
        val options = io.sentry.TransactionOptions().apply { isBindToScope = true }
        val transaction = Sentry.startTransaction(
            "screen.time_to_interactive",
            "Screen Time To Interactive",
            options
        )
        // Sentry.setSpan(transaction) // Removed, not needed

        // Start NTS span using Tracer
        ntsSpan = Tracer.startSpan("navigation.nts") as? ITransaction

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
                delay(1000)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    statusText.text = "All content loaded!"
                    Sentry.reportFullyDisplayed()

                    // Start and finish TTI span using Tracer
                    ttiSpan = Tracer.startSpan("screen.tti") as? ITransaction
                    ttiSpan?.finish(SpanStatus.OK)

                    // Finish NTS span
                    ntsSpan?.finish(SpanStatus.OK)

                    // Finish the transaction after TTFD
                    transaction.finish()
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