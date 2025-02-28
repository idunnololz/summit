package com.idunnololz.summit.util.colorPicker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.idunnololz.summit.util.Utils

class SeekBarBackgroundDrawable(private val drawable: Drawable) : Drawable() {
    private val height: Float
    private val paint: Paint

    init {
        height = Utils.convertDpToPixel(16f)
        paint = Paint()
    }

    override fun draw(canvas: Canvas) {
        val bitmap = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.RGB_565)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(Canvas(bitmap))

        val bounds = bounds
        canvas.clipRect(
            Rect(
                bounds.left,
                (bounds.centerY() - height / 2).toInt(),
                bounds.right,
                (bounds.centerY() + height / 2).toInt()
            )
        )

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }
}