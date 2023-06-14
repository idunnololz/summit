package com.idunnololz.summit.reddit

import android.view.View

/**
 * Represents a view that contains spoilers. Required to support spoilers.
 */
class SpoilerContainer(private val view: View) {
    fun onSpoilerStateChanged() {
        view.invalidate()
    }
}