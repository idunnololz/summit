package com.idunnololz.summit.util.ext

import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

private const val TAG = "NavControllerExt"

fun NavController.navigateSafe(navDirections: NavDirections? = null, navOptions: NavOptions? = null) {
    try {
        navDirections?.let {
            this.navigate(navDirections, navOptions)
        }
    } catch (e: Exception) {
        Log.e(TAG, "", e)
    }
}

fun NavController.navigateSafe(navDirections: NavDirections? = null, extras: Navigator.Extras) {
    try {
        navDirections?.let {
            this.navigate(navDirections, extras)
        }
    } catch (e: Exception) {
        Log.e(TAG, "", e)
    }
}
