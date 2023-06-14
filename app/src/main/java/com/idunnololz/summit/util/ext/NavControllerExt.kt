package com.idunnololz.summit.util.ext

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigator

fun NavController.navigateSafe(navDirections: NavDirections? = null) {
    try {
        navDirections?.let {
            this.navigate(navDirections)
        }
    } catch (e: Exception) {
    }
}

fun NavController.navigateSafe(navDirections: NavDirections? = null, extras: Navigator.Extras) {
    try {
        navDirections?.let {
            this.navigate(navDirections, extras)
        }
    } catch (e: Exception) {
    }
}