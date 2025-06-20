package com.example.androidcustommetricsapp

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Intent
import android.os.Bundle
import com.example.androidcustommetricsapp.Tracer.NTS_OP
import com.example.androidcustommetricsapp.Tracer.SNTR_OP
import com.example.androidcustommetricsapp.Tracer.spans
import com.example.androidcustommetricsapp.Tracer.startSpan
import io.sentry.ISpan
import io.sentry.Instrumenter.SENTRY
import io.sentry.Sentry
import io.sentry.SentryDate
import io.sentry.SentryInstantDate
import io.sentry.TransactionOptions
import io.sentry.android.core.performance.AppStartMetrics
import io.sentry.util.TracingUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MINUTES

object Tracer : ActivityLifecycleCallbacks {
    private const val TTFD_OP = "ui.load.full_display" // Time to interactive
    private const val TTID_OP = "ui.load.initial_display" // Time to first layout
    internal const val UI_LOAD_OP = "ui.load"
    internal const val NTS_OP = "ui.load.nts"
    internal const val SNTR_OP = "ui.load.sntr"
    private const val APP_START_COLD = "app.start.cold"
    private const val APP_START_OP = "app.start.flow"
    val spans = ConcurrentHashMap<String, ISpan>()

    @JvmStatic
    fun startSpan(
        opName: String,
        description: String? = null,
        startDate: SentryDate? = null,
        isRoot: Boolean = false,
        continueTrace: Boolean = false
    ): ISpan {
        val activeSpan = Sentry.getSpan()
        val span = if (activeSpan != null && !isRoot) {
            activeSpan.startChild(opName, description, startDate, SENTRY)
        } else {
            val headers = TracingUtils.trace(Sentry.getCurrentScopes(), null, activeSpan)
            val root = if (continueTrace && headers != null) {
                // Get trace information from the active span
                val trace = Sentry.continueTrace(
                    headers.sentryTraceHeader.value, listOfNotNull(headers.baggageHeader?.value)
                )

                trace?.let {
                    if (description != null) {
                        it.description = description
                        it.name = description
                    }
                    it.operation = opName
                    Sentry.startTransaction(trace, TransactionOptions().apply { startTimestamp = startDate; isBindToScope = true })
                } ?: Sentry.startTransaction(opName, opName, description, TransactionOptions().apply { startTimestamp = startDate; isBindToScope = true })
            } else {
                // Start a root transaction if none exists
                Sentry.startTransaction(description ?: opName, opName, description, TransactionOptions().apply { startTimestamp = startDate; isBindToScope = true })
            }
            root
        }
        return span
    }

    @JvmStatic
    fun stopSpan(span: ISpan) {
        span.finish()
        spans.entries.removeAll { it.value == span }
    }

    // TODO: replace this with Sentry.reportFullyDrawn after we support attaching TTID/TTFD spans to custom txs
    fun reportFullyDrawn(activity: Activity) {
        val key = "${TTFD_OP}_${activity.hashCode()}"
        spans[key]?.let { ttfdSpan ->
            stopSpan(ttfdSpan)
        }

        if (activity::class.java.name.contains("home", ignoreCase = true)) {
            spans[APP_START_OP]?.let { appStartSpan ->
                // If this is the home activity, we consider the app start flow to be completed
                stopSpan(appStartSpan)
            }
        }
    }

    fun startAppFlow() {
        val appStart = AppStartMetrics.getInstance().appStartTimeSpan
        val appStartDate = if (appStart.hasStarted() && appStart.durationMs <= MINUTES.toMillis(1)) {
            appStart.startTimestamp
        } else {
            SentryInstantDate()
        }
        val appStartSpan = startSpan(APP_START_OP, "App start flow", appStartDate)
        spans[APP_START_OP] = appStartSpan
        val appSTISpan = appStartSpan.startChild(APP_START_COLD, "App Start to Interactive", appStartDate, SENTRY)
        spans[APP_START_COLD] = appSTISpan
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // we use class here as a key, because we did not have access to Activity instance when the span was started
        spans["${NTS_OP}_${activity::class.java}"]?.let { ntsSpan ->
            stopSpan(ntsSpan)
        }

        val now = SentryInstantDate()
        val activitySpan = startSpan(UI_LOAD_OP, activity::class.java.simpleName, now, isRoot = true, continueTrace = true)
        spans["${UI_LOAD_OP}_${activity.hashCode()}"] = activitySpan

        val ttidSpan = activitySpan.startChild(TTID_OP, "${activity::class.java.simpleName} first layout", now, SENTRY)
        spans["${TTID_OP}_${activity.hashCode()}"] = ttidSpan

        val ttfdSpan = activitySpan.startChild(TTFD_OP, "${activity::class.java.simpleName} interactive", now, SENTRY)
        spans["${TTFD_OP}_${activity.hashCode()}"] = ttfdSpan
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
        // for simplicity we stop TTID and SNTR spans here, but it has to be more sophisticated
        // see: https://github.com/getsentry/sentry-java/blob/main/sentry-android-core/src/main/java/io/sentry/android/core/internal/util/FirstDrawDoneListener.java

        // we use class here as a key, because we did not have access to Activity instance when the span was started
        spans["${SNTR_OP}_${activity::class.java}"]?.let { sntrSpan ->
            stopSpan(sntrSpan)
        }
       spans["${TTID_OP}_${activity.hashCode()}"]?.let { ttidSpan ->
            stopSpan(ttidSpan)
        }
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        val key = "${UI_LOAD_OP}_${activity.hashCode()}"
        spans[key]?.let { activitySpan ->
            stopSpan(activitySpan)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}

fun Activity.launchTraced(clazz: Class<out Activity>, customize: Intent.() -> Unit = {}) {
    val origin = this
    val now = SentryInstantDate()
    // Start a span for the new activity
    val sntrSpan = startSpan(SNTR_OP, "Screen navigation to ${clazz.simpleName} first layout", now, isRoot = true)
    sntrSpan.setData("screen_origin", origin::class.java.simpleName)
    spans["${SNTR_OP}_${clazz}"] = sntrSpan

    val ntsSpan = sntrSpan.startChild(NTS_OP, "Navigation to ${clazz.simpleName}", now, SENTRY)
    ntsSpan.setData("screen_origin", origin::class.java.simpleName)
    spans["${NTS_OP}_${clazz}"] = ntsSpan

    val intent = Intent(origin, clazz)
    intent.customize()

    startActivity(intent)
}