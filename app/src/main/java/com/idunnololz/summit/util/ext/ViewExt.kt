package com.idunnololz.summit.util.ext

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.graphics.applyCanvas
import androidx.core.view.ViewCompat

fun View.drawToBitmap(
    backgroundColor: Int = Color.TRANSPARENT,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888
): Bitmap {
    if (!ViewCompat.isLaidOut(this)) {
        throw IllegalStateException("View needs to be laid out before calling drawToBitmap()")
    }
    return Bitmap.createBitmap(width, height, config)
        .apply { eraseColor(backgroundColor) }
        .applyCanvas {
            translate(-scrollX.toFloat(), -scrollY.toFloat())
            draw(this)
        }
}

fun View.runAfterLayout(callback: () -> Unit) {
    if (this.isLaidOut) {
        callback()

        return
    }

    this.viewTreeObserver.addOnPreDrawListener(
        object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                this@runAfterLayout.viewTreeObserver.removeOnPreDrawListener(this)
                callback()
                return true
            }
        }
    )
}