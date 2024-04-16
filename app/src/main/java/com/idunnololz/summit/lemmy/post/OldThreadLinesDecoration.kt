package com.idunnololz.summit.lemmy.post

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import arrow.core.left
import com.idunnololz.summit.R
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDimen

class OldThreadLinesDecoration(
    private val context: Context,
    private val isCompactView: Boolean,
    private val colorful: Boolean = false,
    private val dividers: Boolean = false,
) : RecyclerView.ItemDecoration() {

    private val distanceBetweenLinesUnit =
        Utils.convertDpToPixel(1f)
    private val startingPadding =
        context.resources.getDimensionPixelSize(R.dimen.reddit_content_horizontal_padding)
    private val topOverdraw =
        if (isCompactView) {
            context.resources.getDimensionPixelSize(R.dimen.comment_top_overdraw_compact)
        } else {
            context.resources.getDimensionPixelSize(R.dimen.comment_top_overdraw)
        }

    private val lineColors = listOf(
        Color.parseColor("#D32F2F"),
        Color.parseColor("#F57C00"),
        Color.parseColor("#FBC02D"),
        Color.parseColor("#388E3C"),
        Color.parseColor("#1976D2"),
        Color.parseColor("#7B1FA2"),
    )

    private val linePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorThreadLines)
        strokeWidth = Utils.convertDpToPixel(2f)
    }
    private val dividerPaint = Paint().apply {
        val color1 = context.getColorFromAttribute(
            com.google.android.material.R.attr.colorOnSurface,
        )
        val color2 = context.getColorFromAttribute(
            com.google.android.material.R.attr.backgroundColor,
        )

        color = ColorUtils.blendARGB(color1, color2, 0.88f)
        strokeWidth = Utils.convertDpToPixel(1f)
    }
    private val textPaint = Paint().apply {
        color = context.getColorCompat(R.color.colorTextFaint)
        textSize = context.getDimen(R.dimen.label_text_size).toFloat()
        isAntiAlias = true
    }
    private val textBackgroundPaint = Paint().apply {
        color = context.getColorFromAttribute(com.google.android.material.R.attr.colorSurface)
        isAntiAlias = true
    }
    private val screenshotWidth = context.getDimen(R.dimen.screenshot_options_size)

    private val tempRect = Rect()

    private fun getColorForDepth(depth: Int): Int {
        return lineColors[depth % lineColors.size]
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount

        for (i in 0 until childCount) {
            val view = parent.getChildAt(i)
            val previousChild = if (i == 0) {
                null
            } else {
                parent.getChildAt(i - 1)
            }
            val previousTag = previousChild?.tag
            val tag = view.tag
            val screenshotMode = view.getTag(R.id.screenshot_mode) as? Boolean == true
            var translationX = view.translationX
            val translationY = view.translationY
            val drawDividerAbove =
                !(previousChild != null && previousTag !is ThreadLinesData) && dividers

            if (screenshotMode) {
                translationX += screenshotWidth
            }

            val topOverdraw =
                if (previousChild != null && previousTag !is ThreadLinesData) {
                    // Do not overdraw above if the element above is not a comment!
                    0
                } else {
                    topOverdraw
                }
            val threadLinesData: ThreadLinesData? = when (tag) {
                is ThreadLinesData -> {
                    tag
                }
                else -> {
                    null
                }
            }

            threadLinesData ?: continue

            val totalDepth = threadLinesData.depth - threadLinesData.baseDepth

            for (lineIndex in 0 until totalDepth.coerceAtMost(threadLinesData.maxDepth)) {
                if (colorful) {
                    linePaint.color = getColorForDepth(lineIndex)
                    linePaint.alpha = (view.alpha * 255).toInt()
                }

                val indent = view.left + (lineIndex.toFloat()) * distanceBetweenLinesUnit *
                    threadLinesData.indentationPerLevel + startingPadding
                c.drawLine(
                    indent + translationX,
                    view.top.toFloat() + translationY,
                    indent + translationX,
                    view.bottom.toFloat() + translationY,
                    linePaint,
                )
            }

            if (totalDepth > threadLinesData.maxDepth) {
                // If we are approaching the max depth, draw the depth

                val textToDraw = (threadLinesData.depth + 1).toString()
                textPaint.getTextBounds(textToDraw, 0, textToDraw.length, tempRect)
                val x = view.left + (threadLinesData.maxDepth - 1) * distanceBetweenLinesUnit *
                    threadLinesData.indentationPerLevel + startingPadding
                val textX = x + translationX - tempRect.width() +
                    Utils.convertDpToPixel(8f)
                val textY = view.top.toFloat() + tempRect.height() + Utils.convertDpToPixel(22f)
                val textPadding = Utils.convertDpToPixel(4f)
                val cornerRadius = Utils.convertDpToPixel(4f)

                c.drawRoundRect(
                    textX - textPadding,
                    textY - tempRect.height() - textPadding,
                    textX + tempRect.width() + textPadding,
                    textY + textPadding,
                    cornerRadius,
                    cornerRadius,
                    textBackgroundPaint,
                )
                c.drawText(textToDraw, textX, textY, textPaint)
            }

            if (drawDividerAbove) {
                dividerPaint.alpha = (view.alpha * 255).toInt()

                val x = view.left + (totalDepth - 1) * distanceBetweenLinesUnit *
                    threadLinesData.indentationPerLevel + startingPadding +
                    (linePaint.strokeWidth)
                val y = view.top.toFloat() + translationY
                // Don't transform dividers by X due to swipe actions
                val start = x - (linePaint.strokeWidth / 2)
                val end = view.right.toFloat()
                dividerPaint.alpha = 255
                c.drawLine(
                    start,
                    y,
                    end,
                    y,
                    dividerPaint,
                )
            }
        }
    }
}
