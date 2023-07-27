package com.idunnololz.summit.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import com.idunnololz.summit.R

class TabsImageButton : AppCompatImageButton {

    private val paintText = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorTextTitle)
        isAntiAlias = true
        textSize = context.resources.getDimension(R.dimen.tabs_text_size)
        textAlign = Paint.Align.CENTER
    }

    private var numTabs: Int = 0
    private var textHeight: Int = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    fun setTabs(tabs: Int) {
        numTabs = tabs

        remeasure()
        invalidate()
    }

    private fun remeasure() {
        val text = numTabs.toString()
        val rect = Rect()
        paintText.getTextBounds(text, 0, text.length, rect)
        textHeight = rect.height()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return
        if (numTabs <= 0) return

        val xPos = width / 2
        val yPos = (canvas.height / 2) + (textHeight / 2)

        // ((textPaint.descent() + textPaint.ascent()) / 2) is the distance from the baseline to the center.

        // ((textPaint.descent() + textPaint.ascent()) / 2) is the distance from the baseline to the center.
        canvas.drawText(numTabs.toString(), xPos.toFloat(), yPos.toFloat(), paintText)
    }
}
