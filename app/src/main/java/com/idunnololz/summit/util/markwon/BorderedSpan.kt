package com.idunnololz.summit.util.markwon

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import com.idunnololz.summit.R
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorCompat

class BorderedSpan(context: Context) : ReplacementSpan() {
    val paintBorder: Paint

//    val mPaintBackground: Paint
    var width: Int = 0
    var r: Resources
    val textColor = context.getColorCompat(R.color.colorTextInverted)
    val padding = Utils.convertDpToPixel(4f)

    init {
        paintBorder = Paint()
        paintBorder.style = Paint.Style.FILL
        paintBorder.isAntiAlias = true
        paintBorder.strokeWidth = Utils.convertDpToPixel(2f)
        paintBorder.setColor(context.getColorCompat(R.color.colorTextTitle))
        paintBorder.alpha = 240

//        mPaintBackground = Paint()
//        mPaintBackground.style = Paint.Style.FILL
//        mPaintBackground.isAntiAlias = true

        r = context.resources

//        mPaintBackground.setColor(Color.GREEN)
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        // return text with relative to the Paint
        width = paint.measureText(text, start, end).toInt()
        return (width + padding * 2).toInt()
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
//        canvas.drawRect(x, top.toFloat(), x + mWidth, bottom.toFloat(), mPaintBackground)
        canvas.drawRect(
            x,
            top.toFloat(),
            x + width + padding + padding,
            bottom.toFloat(),
            paintBorder,
        )
        paint.setColor(textColor) // use the default text paint to preserve font size/style
        canvas.drawText(text, start, end, x + padding, y.toFloat(), paint)
    }
}
