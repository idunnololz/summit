package com.idunnololz.summit.util.ext

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat

fun Context.getColorFromAttribute(attribute: Int): Int {
    val attributes = obtainStyledAttributes(intArrayOf(attribute))
    val color = attributes.getColor(0, 0)
    attributes.recycle()
    return color
}

fun Context.getDimen(@DimenRes dimen: Int): Int {
    return resources.getDimension(dimen).toInt()
}

fun Context.getColorCompat(@ColorRes color: Int): Int =
    ContextCompat.getColor(this, color)