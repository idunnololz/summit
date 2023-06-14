package com.idunnololz.summit.util

import android.graphics.Rect
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View

class GridSpaceItemDecoration(
    private val space: Int,
    private val spaceAboveFirstAndBelowLastItem: Boolean,
    private val spaceBeforeStartAndAfterEnd: Boolean
) : RecyclerView.ItemDecoration() {

    private var needLeftSpacing = false

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val layoutManager = parent.layoutManager
        val gridSize = (layoutManager as GridLayoutManager).spanCount

        val frameWidth = ((parent.width - space.toFloat() * (gridSize - 1)) / gridSize).toInt()
        val padding = parent.width / gridSize - frameWidth
        val itemPosition = (view.layoutParams as RecyclerView.LayoutParams).viewAdapterPosition

        outRect.top = space
        outRect.bottom = 0
        if (!spaceAboveFirstAndBelowLastItem) {
            if (itemPosition < gridSize) {
                outRect.top = 0
            }
        } else {
            val lastRow = ((parent.adapter?.itemCount ?: 0) - 1) / gridSize
            val curRow = itemPosition / gridSize
            if (lastRow == curRow) {
                outRect.bottom = space
            }
        }

        if (itemPosition % gridSize == 0 && (itemPosition + 1) % gridSize == 0) {
            // Special case for when there is 1 item in this row
            needLeftSpacing = false
            if (spaceBeforeStartAndAfterEnd) {
                outRect.left = space
                outRect.right = space
            } else {
                outRect.left = 0
                outRect.right = 0
            }
        } else if (itemPosition % gridSize == 0) {
            if (spaceBeforeStartAndAfterEnd) {
                outRect.left = space
            } else {
                outRect.left = 0
            }
            outRect.right = padding
            needLeftSpacing = true
        } else if ((itemPosition + 1) % gridSize == 0) {
            needLeftSpacing = false
            if (spaceBeforeStartAndAfterEnd) {
                outRect.right = space
            } else {
                outRect.right = 0
            }
            outRect.left = padding
        } else if (needLeftSpacing) {
            needLeftSpacing = false
            outRect.left = space - padding
            if ((itemPosition + 2) % gridSize == 0) {
                outRect.right = space - padding
            } else {
                outRect.right = space / 2
            }
        } else if ((itemPosition + 2) % gridSize == 0) {
            needLeftSpacing = false
            outRect.left = space / 2
            outRect.right = space - padding
        } else {
            needLeftSpacing = false
            outRect.left = space / 2
            outRect.right = space / 2
        }
    }
}