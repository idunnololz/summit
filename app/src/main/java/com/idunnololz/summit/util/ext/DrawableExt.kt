package com.idunnololz.summit.util.ext

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat
import coil.drawable.ScaleDrawable
import com.idunnololz.summit.util.Size

fun Drawable.tint(color: Int): Drawable {
    val wrappedDrawable: Drawable = DrawableCompat.wrap(this)
    DrawableCompat.setTint(wrappedDrawable, color)
    return wrappedDrawable
}

fun Drawable.getSize(outSize: Size) {
    when (this) {
        is BitmapDrawable -> {
            outSize.width = this.bitmap.width
            outSize.height = this.bitmap.height
        }

        is ScaleDrawable -> {
            this.child.getSize(outSize)
        }

        else -> {
            outSize.width = intrinsicWidth
            outSize.height = intrinsicHeight
        }
    }
}