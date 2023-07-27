package com.idunnololz.summit.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import com.idunnololz.summit.util.Utils

class HorizontalDividerSpan : ReplacementSpan() {

    private val dividerTotalSize = Utils.convertDpToPixel(8f).toInt()
    private val dividerSize = Utils.convertDpToPixel(2f)

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int = dividerTotalSize

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val oldStyle = paint.style

        paint.style = Paint.Style.FILL
        val cx = x + dividerTotalSize / 2
        val cy = (top + bottom) / 2
        canvas.drawCircle(cx, cy.toFloat(), dividerSize, paint)

        paint.style = oldStyle
    }
}
