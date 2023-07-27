package com.idunnololz.summit.lemmy.post

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDimen

class ModernThreadLinesDecoration(
    private val context: Context,
    private val isCompactView: Boolean,
) : RecyclerView.ItemDecoration() {

    private val distanceBetweenLinesUnit =
        Utils.convertDpToPixel(1f)
    private val startingPadding =
        context.resources.getDimensionPixelSize(R.dimen.reddit_content_horizontal_padding)
    private val lineMargin = context.getDimen(R.dimen.padding_half)

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
        val color1 = context.getColorFromAttribute(com.google.android.material.R.attr.colorOnSurface)
        val color2 = context.getColorFromAttribute(com.google.android.material.R.attr.backgroundColor)

        color = ColorUtils.blendARGB(color1, color2, 0.88f)
        strokeWidth = Utils.convertDpToPixel(1f)
    }

    fun getColorForDepth(depth: Int): Int {
        if (depth == 0) {
            return Color.TRANSPARENT
        }
        return lineColors[(depth - 1) % lineColors.size]
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        context.getColorFromAttribute(R.attr.border_color)
        val childCount = parent.childCount

        for (i in 0 until childCount) {
            val view = parent.getChildAt(i)
            val previousChild = if (i == 0) {
                null
            } else {
                parent.getChildAt(i - 1)
            }
            val previousTag = previousChild?.tag
            val drawDividerAbove =
                if (previousChild != null && previousTag !is ThreadLinesData) {
                    // Do not overdraw above if the element above is not a comment!
                    false
                } else {
                    true
                }

            val tag = view.tag
            val translationX = view.translationX
            val translationY = view.translationY

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
            val indent = (totalDepth.toFloat() - 1f) * distanceBetweenLinesUnit *
                threadLinesData.indentationPerLevel
            val x = view.left + indent + startingPadding + (linePaint.strokeWidth / 2)

            if (totalDepth > 0) {
                linePaint.color = getColorForDepth(totalDepth)

                linePaint.alpha = (view.alpha * 255).toInt()

                c.drawLine(
                    x + translationX,
                    view.top.toFloat() + translationY + lineMargin,
                    x + translationX,
                    view.bottom.toFloat() + translationY - lineMargin,
                    linePaint,
                )
            }

            if (drawDividerAbove) {
                dividerPaint.alpha = (view.alpha * 255).toInt()

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
