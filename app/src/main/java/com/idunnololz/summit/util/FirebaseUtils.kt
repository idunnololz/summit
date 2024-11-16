package com.idunnololz.summit.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

var isFirebaseInitialized: Boolean = false

val crashlytics: FirebaseCrashlytics?
    get() {
        return if (isFirebaseInitialized) {
            try {
                FirebaseCrashlytics.getInstance()
            } catch (e: Exception) {
                Log.e("Crashlytics", "Unable to get crashlytics", e)
                null
            }
        } else {
            null
        }
    }
