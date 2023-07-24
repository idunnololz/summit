package com.idunnololz.summit.util.ext

import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat

fun Drawable.tint(color: Int): Drawable {
    val wrappedDrawable: Drawable = DrawableCompat.wrap(this)
    DrawableCompat.setTint(wrappedDrawable, color)
    return wrappedDrawable
}
