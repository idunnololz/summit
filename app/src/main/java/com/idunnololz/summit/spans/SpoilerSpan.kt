package com.idunnololz.summit.spans

import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

class SpoilerSpan(
    private val spoilerColor: Int,
    private val spoilerRevealedColor: Int,
    private val textColor: Int,
) : ClickableSpan() {

    var showSpoiler = false

    override fun onClick(widget: View) {
        // Toggle the shown state
        showSpoiler = !showSpoiler

        widget.requestLayout()
        widget.invalidate()
    }

    override fun updateDrawState(ds: TextPaint) {
        // Don't call the super method otherwise this may override our settings!
        super.updateDrawState(ds)

        ds.isUnderlineText = false
        if (showSpoiler) {
            ds.color = textColor
            ds.bgColor = spoilerRevealedColor
        } else {
            // Spoiler is not shown, make the text color the same as the background color
            ds.color = Color.TRANSPARENT
            ds.bgColor = spoilerColor
        }
    }
}

// class SpoilerReplacementSpan(
//    private val spoilerColor: Int,
//    private val textColor: Int
// ) : ReplacementSpan() {
//
//    var showSpoiler = false
//
//    companion object {
//        private val CORNER_RADIUS = Utils.convertDpToPixel(3f)
//        private val startEndPadding = Utils.convertDpToPixel(4f)
//    }
//
//    override fun getSize(
//        paint: Paint,
//        text: CharSequence?,
//        start: Int,
//        end: Int,
//        fm: Paint.FontMetricsInt?
//    ): Int = (paint.measureText(text, start, end) + startEndPadding * 2).roundToInt()
//
//    override fun draw(
//        canvas: Canvas,
//        text: CharSequence,
//        start: Int,
//        end: Int,
//        x: Float,
//        top: Int,
//        y: Int,
//        bottom: Int,
//        paint: Paint
//    ) {
//        if (showSpoiler) {
//            super.updateDrawState()
//        } else {
//            val rect = RectF(
//                x,
//                top.toFloat(),
//                x + measureText(paint, text, start, end) + startEndPadding * 2,
//                bottom.toFloat()
//            )
//            paint.color = spoilerColor
//            canvas.drawRoundRect(
//                rect,
//                CORNER_RADIUS,
//                CORNER_RADIUS,
//                paint
//            )
//            paint.color = textColor
//            canvas.drawText(text, start, end, x + startEndPadding, y.toFloat(), paint)
//        }
//    }
//
//    private fun measureText(
//        paint: Paint,
//        text: CharSequence,
//        start: Int,
//        end: Int
//    ): Float {
//        return paint.measureText(text, start, end)
//    }
// }
