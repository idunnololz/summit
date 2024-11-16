package com.idunnololz.summit.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GridSpaceItemDecoration(
    space: Int,
    private val spaceAboveFirstAndBelowLastItem: Boolean,
    private val horizontalSpaceOnFirstAndLastItem: Boolean = true,
    private val hasSameSpans: Boolean = true,
) : RecyclerView.ItemDecoration() {

    var verticalSpace = space
    var horizontalSpace = space

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val layoutManager = parent.layoutManager
        val spanCount = (layoutManager as GridLayoutManager).spanCount

        val itemPosition = (view.layoutParams as RecyclerView.LayoutParams).bindingAdapterPosition
        val column = itemPosition % spanCount // item column

        if (hasSameSpans) {
            outRect.top = verticalSpace
        }
        outRect.bottom = 0
        if (!spaceAboveFirstAndBelowLastItem) {
            if (itemPosition < spanCount) {
                outRect.top = 0
            }
        } else {
            if (hasSameSpans) {
                val lastRow = ((parent.adapter?.itemCount ?: 0) - 1) / spanCount
                val curRow = itemPosition / spanCount
                if (lastRow == curRow) {
                    outRect.bottom = verticalSpace
                }
            }
        }

        outRect.left = horizontalSpace / 2
        outRect.right = horizontalSpace / 2

        if (column == 0 && horizontalSpaceOnFirstAndLastItem && hasSameSpans) {
            outRect.left = 0
        }
        if (column == spanCount - 1 && horizontalSpaceOnFirstAndLastItem && hasSameSpans) {
            outRect.right = 0
        }
    }
}
