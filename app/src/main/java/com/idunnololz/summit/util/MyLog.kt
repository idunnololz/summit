package com.idunnololz.summit.util

import com.google.firebase.crashlytics.FirebaseCrashlytics

object MyLog {
    fun d(tag: String, message: String) = FirebaseCrashlytics.getInstance().log("[$tag] $message")
}