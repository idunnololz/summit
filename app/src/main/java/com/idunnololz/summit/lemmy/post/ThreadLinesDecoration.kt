package com.idunnololz.summit.lemmy.post

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.util.Utils

class ThreadLinesDecoration(
    private val context: Context
) : RecyclerView.ItemDecoration() {

    val distanceBetweenLines =
        context.resources.getDimensionPixelSize(R.dimen.thread_line_total_size)
    val startingPadding =
        context.resources.getDimensionPixelSize(R.dimen.reddit_content_horizontal_padding)
    val topOverdraw = context.resources.getDimensionPixelSize(R.dimen.comment_top_overdraw)

    private val linePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorThreadLines)
        strokeWidth = Utils.convertDpToPixel(2f)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount

        for (i in 0 until childCount) {
            val view = parent.getChildAt(i)
            val lastTag = if (i == 0) {
                null
            } else {
                parent.getChildAt(i - 1).tag
            }
            val tag = view.tag
            val translationX = view.translationX
            val translationY = view.translationY
            val topOverdraw = topOverdraw
            val totalDepth = when (tag) {
                is PostFragment.Item.CommentItem -> {
                    tag.depth - tag.baseDepth
                }
                is PostFragment.Item.PendingCommentItem -> {
                    tag.depth - tag.baseDepth
                }
                is PostFragment.Item.MoreCommentsItem -> {
                    tag.depth - tag.baseDepth
                }
                else -> {
                    -1
                }
            }

            if (totalDepth == -1) continue

            for (lineIndex in 0 until totalDepth) {
                val x =
                    view.left + (lineIndex.toFloat()) * distanceBetweenLines + startingPadding
                c.drawLine(
                    x + translationX,
                    view.top.toFloat() - topOverdraw + translationY,
                    x + translationX,
                    view.bottom.toFloat() + translationY,
                    linePaint
                )
            }
        }
    }
}