package com.idunnololz.summit.util.coil

import android.util.Log
import coil3.request.NullRequestDataException
import coil3.util.Logger

class BetterDebugLogger @JvmOverloads constructor(
    override var minLevel: Logger.Level = Logger.Level.Debug,
) : Logger {

    override fun log(tag: String, level: Logger.Level, message: String?, throwable: Throwable?) {
        if (message != null) {
            Log.println(level.toInt(), tag, message)
        }
        if (throwable != null && throwable !is NullRequestDataException) {
            Log.println(level.toInt(), tag, throwable.stackTraceToString())
        }
    }
}

fun Logger.Level.toInt() = when (this) {
    Logger.Level.Verbose -> Log.VERBOSE
    Logger.Level.Debug -> Log.DEBUG
    Logger.Level.Info -> Log.INFO
    Logger.Level.Warn -> Log.WARN
    Logger.Level.Error -> Log.ERROR
}

fun Logger.log(tag: String, throwable: Throwable) {
    if (minLevel <= Logger.Level.Error) {
        log(tag, Logger.Level.Error, null, throwable)
    }
}

inline fun Logger.log(tag: String, throwable: Throwable, message: () -> String) {
    if (minLevel <= Logger.Level.Error) {
        log(tag, Logger.Level.Error, message(), throwable)
    }
}

inline fun Logger.log(tag: String, level: Logger.Level, message: () -> String) {
    if (minLevel <= level) {
        log(tag, level, message(), null)
    }
}
