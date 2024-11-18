package com.idunnololz.summit.util

import android.graphics.Rect
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout

fun View.excludeRegionFromSystemGestures() {
    val that = this
    doOnLayout {
        ViewCompat.setSystemGestureExclusionRects(
            that,
            listOf(
                Rect().apply {
                    that.getDrawingRect(this)
                }
            )
        )
    }
}

fun View.clearExcludeRegionFromSystemGestures() {
    val that = this
    doOnLayout {
        ViewCompat.setSystemGestureExclusionRects(
            that,
            listOf()
        )
    }
}