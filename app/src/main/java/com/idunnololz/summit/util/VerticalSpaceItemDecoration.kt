package com.idunnololz.summit.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class VerticalSpaceItemDecoration(
    private val verticalSpaceHeight: Int,
    private val hasStartAndEndSpace: Boolean,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (hasStartAndEndSpace && position == 0 || position == -1) {
            outRect.top = verticalSpaceHeight
        } else if (position > 0) {
            outRect.top = verticalSpaceHeight
        }
        if (hasStartAndEndSpace && position == (parent.adapter?.itemCount ?: 0) - 1) {
            outRect.bottom = verticalSpaceHeight
        }
    }
}
