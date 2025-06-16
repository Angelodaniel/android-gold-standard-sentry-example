package com.example.androidcustommetricsapp

import io.sentry.ITransaction
import io.sentry.ISpan
import io.sentry.Sentry
import io.sentry.TransactionOptions
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

object Tracer {
    private val lastTrace = AtomicReference<ISpan?>()
    val appLaunchSpan = AtomicReference<ISpan?>()
    val spans = ConcurrentHashMap<String, ISpan>()

    @JvmStatic
    fun startSpan(opName: String): ISpan {
        val activeSpan = Sentry.getSpan()
        val span = if (activeSpan != null) {
            activeSpan.startChild(opName)
        } else {
            // Start a root transaction if none exists
            val root = Sentry.startTransaction(opName, opName, TransactionOptions().apply { isBindToScope = true })
            lastTrace.set(root)
            root
        }
        spans[opName] = span
        return span
    }

    @JvmStatic
    fun stopSpan(opName: String) {
        spans[opName]?.finish()
        spans.remove(opName)
    }

    @JvmStatic
    fun stopSpan(span: ISpan) {
        span.finish()
        spans.entries.removeIf { it.value == span }
    }
} 