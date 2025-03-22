package com.idunnololz.summit.util.crashLogger

import android.util.Log
import io.sentry.Sentry

var isCrashLoggerInitialized: Boolean = false

class CrashLogger {

    companion object {
        @Volatile private var instance: CrashLogger? = null // Volatile modifier is necessary

        fun getInstance() =
            instance ?: synchronized(this) { // synchronized to avoid concurrency problem
                instance ?: CrashLogger().also { instance = it }
            }
    }

    fun recordException(e: Throwable) {
        Sentry.captureException(e)
    }
}

val crashLogger: CrashLogger?
    get() {
        return if (isCrashLoggerInitialized) {
            try {
                CrashLogger.getInstance()
            } catch (e: Exception) {
                Log.e("CrashLogger", "Unable to get CrashLogger", e)
                null
            }
        } else {
            null
        }
    }
