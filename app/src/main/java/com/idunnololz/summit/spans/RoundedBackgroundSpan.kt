package com.idunnololz.summit.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.RectF
import android.text.style.ReplacementSpan
import com.idunnololz.summit.util.Utils
import kotlin.math.roundToInt

class RoundedBackgroundSpan(
    private var backgroundColor: Int,
    private var textColor: Int,
) : ReplacementSpan() {
    companion object {
        private val CORNER_RADIUS = Utils.convertDpToPixel(3f)
        private val startEndPadding = Utils.convertDpToPixel(4f)
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val rect = RectF(
            x,
            top.toFloat(),
            x + measureText(paint, text, start, end) + startEndPadding * 2,
            bottom.toFloat(),
        )
        paint.color = backgroundColor
        canvas.drawRoundRect(
            rect,
            CORNER_RADIUS,
            CORNER_RADIUS,
            paint,
        )
        paint.color = textColor
        canvas.drawText(text, start, end, x + startEndPadding, y.toFloat(), paint)
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: FontMetricsInt?,
    ): Int {

        if (fm != null) {
            fm.top = paint.fontMetrics.top.toInt()
            fm.bottom = paint.fontMetrics.bottom.toInt()
            fm.descent = paint.fontMetrics.descent.toInt()
            fm.ascent = paint.fontMetrics.ascent.toInt()
        }

        return (measureText(paint, text ?: "", start, end) + startEndPadding * 2).roundToInt()
    }

    private fun measureText(paint: Paint, text: CharSequence, start: Int, end: Int): Float {
        return paint.measureText(text, start, end)
    }
}
