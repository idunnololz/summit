package com.idunnololz.summit.settings.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class HorizontalSpaceItemDecoration(
    private val sizePx: Int,
    private val startAndEndSpacePx: Int,
) : ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position <= 0) { // position can be negative if the view is being removed via animation
            outRect.left = startAndEndSpacePx
        }
        if (position != (parent.adapter?.itemCount ?: 0) - 1) {
            outRect.right = sizePx
        } else {
            outRect.right = startAndEndSpacePx
        }
    }
}
