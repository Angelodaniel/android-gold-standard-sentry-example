package com.example.androidcustommetricsapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.sentry.Sentry
import io.sentry.Span
import io.sentry.ITransaction
import io.sentry.TransactionOptions
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlinx.coroutines.*
import java.util.*
import io.sentry.ISpan
import io.sentry.SentryDate
import io.sentry.SentryInstantDate
import java.time.Instant

class AutoTTIDTTFDWithNTSMeasurementActivity : AppCompatActivity() {
    private val client by lazy { OkHttpClient() }
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    Tracer.reportFullyDrawn(this@AutoTTIDTTFDWithNTSMeasurementActivity)
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
        scope.cancel()
        super.onDestroy()
    }
} 