package com.idunnololz.summit.util.colorPicker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.idunnololz.summit.util.Utils

/**
 * The AlphaColorDrawable draws a color with a tiled
 * white/gray background representing alpha or transparency.
 * Aside from the tiled background, it functions the same
 * as any ColorDrawable.
 */
class AlphaColorDrawable(@ColorInt color: Int) : Drawable() {
    private val bitmapPaint: Paint
    private val paint: Paint

    init {
        val size: Int = Utils.convertDpToPixel(8f).toInt()
        bitmapPaint = Paint()
        bitmapPaint.color = Color.LTGRAY

        if (tile == null || tile!!.isRecycled) {
            tile = Bitmap.createBitmap(size * 4, size * 4, Bitmap.Config.RGB_565)
            val canvas = Canvas(tile!!)
            canvas.drawColor(Color.WHITE)
            var x = 0
            while (x < canvas.width) {
                var y = if (x % (size * 2) == 0) 0 else size
                while (y < canvas.width) {
                    canvas.drawRect(
                        x.toFloat(),
                        y.toFloat(),
                        (x + size).toFloat(),
                        (y + size).toFloat(),
                        bitmapPaint,
                    )
                    y += size * 2
                }
                x += size
            }
        }

        paint = Paint()
        paint.color = color
    }

    override fun draw(canvas: Canvas) {
        val b = bounds

        if (paint.alpha < 255) {
            var x = b.left
            while (x < b.right) {
                var y = b.top
                while (y < b.bottom) {
                    canvas.drawBitmap(tile!!, x.toFloat(), y.toFloat(), bitmapPaint)
                    y += tile!!.height
                }
                x += tile!!.width
            }
        }

        canvas.drawRect(
            b.left.toFloat(),
            b.top.toFloat(),
            b.right.toFloat(),
            b.bottom.toFloat(),
            paint,
        )
    }

    override fun setAlpha(alpha: Int) {
        val color = bitmapPaint.color
        bitmapPaint.color = Color.argb(
            alpha,
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    companion object {
        var tile: Bitmap? = null
    }
}
