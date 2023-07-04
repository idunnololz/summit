package com.idunnololz.summit.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

object MyLog {
    fun d(tag: String, message: String) {
        FirebaseCrashlytics.getInstance().log("[$tag] $message")
        Log.d(tag, message)
    }
}