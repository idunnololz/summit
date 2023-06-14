package com.idunnololz.summit.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan

/**
 * android.text.style.QuoteSpan hard-codes the strip color and gap. :(
 */
class CustomQuoteSpan(
    private val backgroundColor: Int,
    private val stripeColor: Int,
    private val stripeWidth: Float,
    private val gap: Float
) : LeadingMarginSpan, LineBackgroundSpan {

    override fun getLeadingMargin(first: Boolean): Int {
        return (stripeWidth + gap).toInt()
    }

    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout
    ) {
        val style = p.style
        val paintColor = p.color
        p.style = Paint.Style.FILL
        p.color = stripeColor
        c.drawRect(x.toFloat(), top.toFloat(), x + dir * stripeWidth, bottom.toFloat(), p)
        p.style = style
        p.color = paintColor
    }

    override fun drawBackground(
        c: Canvas,
        p: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lnum: Int
    ) {
        val paintColor = p.color
        p.color = backgroundColor
        c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), p)
        p.color = paintColor
    }

}