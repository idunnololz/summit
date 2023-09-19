package com.idunnololz.summit.lemmy.post

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getDimen

class OldThreadLinesDecoration(
    private val context: Context,
    private val isCompactView: Boolean,
    private val colorful: Boolean = false,
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
    private val screenshotWidth = context.getDimen(R.dimen.screenshot_options_size)

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

            for (lineIndex in 0 until totalDepth) {
                if (colorful) {
                    linePaint.color = getColorForDepth(lineIndex)
                    linePaint.alpha = (view.alpha * 255).toInt()
                }

                val indent = view.left + (lineIndex.toFloat()) * distanceBetweenLinesUnit *
                    threadLinesData.indentationPerLevel + startingPadding
                c.drawLine(
                    indent + translationX,
                    view.top.toFloat() - topOverdraw + translationY,
                    indent + translationX,
                    view.bottom.toFloat() + translationY,
                    linePaint,
                )
            }
        }
    }
}
