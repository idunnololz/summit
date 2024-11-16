package com.idunnololz.summit.util

import android.content.Context
import android.util.TypedValue
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GridAutofitLayoutManager : GridLayoutManager {

    companion object {
        @Suppress("unused")
        private val TAG = GridAutofitLayoutManager::class.java.simpleName
    }

    private var columnWidth: Int = 0
    private var columnWidthChanged = true

    private var lastTotalSpace: Int = 0

    // Initially set spanCount to 1, will be changed automatically later.
    constructor(context: Context, columnWidth: Int) : super(context, 1) {
        setColumnWidth(checkedColumnWidth(context, columnWidth))
    }

    // Initially set spanCount to 1, will be changed automatically later.
    @Suppress("unused")
    constructor(
        context: Context,
        columnWidth: Int,
        orientation: Int,
        reverseLayout: Boolean,
    ) : super(context, 1, orientation, reverseLayout) {
        setColumnWidth(checkedColumnWidth(context, columnWidth))
    }

    private fun checkedColumnWidth(context: Context, columnWidth: Int): Int {
        return if (columnWidth <= 0) {
            /* Set default columnWidth value (48dp here). It is better to move this constant
            to static constant on top, but we need context to convert it to dp, so can't really
            do so. */
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                48f,
                context.resources.displayMetrics,
            ).toInt()
        } else {
            columnWidth
        }
    }

    private fun setColumnWidth(newColumnWidth: Int) {
        if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
            columnWidth = newColumnWidth
            columnWidthChanged = true
        }
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
        val totalSpace: Int = if (orientation == VERTICAL) {
            width - paddingRight - paddingLeft
        } else {
            height - paddingTop - paddingBottom
        }
        if (columnWidthChanged && columnWidth > 0 || totalSpace != lastTotalSpace) {
            val spanCount = 1.coerceAtLeast(totalSpace / columnWidth)
            setSpanCount(spanCount)
            columnWidthChanged = false
            lastTotalSpace = totalSpace
        }
        super.onLayoutChildren(recycler, state)
    }
}
