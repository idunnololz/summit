package com.idunnololz.summit.util.colorPicker

import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import com.idunnololz.summit.util.Utils

class SeekBarDrawable(drawable: Drawable?) :
    ClipDrawable(drawable, Gravity.START, HORIZONTAL) {
    private val height: Float
    private var rect: Rect? = null

    init {
        height = Utils.convertDpToPixel(16f)
    }

    override fun draw(canvas: Canvas) {
        if (rect == null) {
            val bounds = bounds
            setBounds(
                Rect(
                    bounds.left,
                    (bounds.centerY() - height / 2).toInt(),
                    bounds.right,
                    (bounds.centerY() + height / 2).toInt()
                ).also { rect = it })
        }

        super.draw(canvas)
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}