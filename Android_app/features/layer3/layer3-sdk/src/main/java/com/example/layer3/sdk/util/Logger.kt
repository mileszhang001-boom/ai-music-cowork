package com.example.layer3.sdk.util

import android.util.Log

object Logger {
    private const val TAG = "Layer3SDK"
    private var isEnabled = true
    private var minLevel = LogLevel.DEBUG

    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun setMinLevel(level: LogLevel) {
        minLevel = level
    }

    fun v(message: String) {
        if (isEnabled && minLevel <= LogLevel.VERBOSE) {
            Log.v(TAG, message)
        }
    }

    fun d(message: String) {
        if (isEnabled && minLevel <= LogLevel.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun i(message: String) {
        if (isEnabled && minLevel <= LogLevel.INFO) {
            Log.i(TAG, message)
        }
    }

    fun w(message: String) {
        if (isEnabled && minLevel <= LogLevel.WARN) {
            Log.w(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (isEnabled && minLevel <= LogLevel.ERROR) {
            if (throwable != null) {
                Log.e(TAG, message, throwable)
            } else {
                Log.e(TAG, message)
            }
        }
    }
}
