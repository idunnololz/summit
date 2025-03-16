package com.idunnololz.summit.lemmy.community

import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OnScrollMarkPostAsReadScrollListener(
    private val getAdapter: () -> PostListAdapter?,
    private val layoutManager: LinearLayoutManager
) : RecyclerView.OnScrollListener() {

    var cacheFirstVisible = -1
    var cacheRangeStart = -1
    var cacheRangeEnd = -1

    fun resetCache() {
        cacheFirstVisible = -1
        cacheRangeStart = -1
        cacheRangeEnd = -1
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
        val adapter = getAdapter() ?: return

        if (cacheFirstVisible == firstVisibleItem) {
            return
        }

        val firstCompletelyVisibleItem =
            layoutManager.findFirstCompletelyVisibleItemPosition()
        val lastCompletelyVisibleItem =
            layoutManager.findLastCompletelyVisibleItemPosition()

        if (firstVisibleItem > -1) {
            adapter.markItemPositionSeen(firstVisibleItem)
            cacheFirstVisible = firstVisibleItem
        }

        if (firstCompletelyVisibleItem != -1) {
            if (cacheRangeStart == firstCompletelyVisibleItem &&
                cacheRangeEnd == lastCompletelyVisibleItem) {
                return
            }

            val range = firstCompletelyVisibleItem..lastCompletelyVisibleItem

            range.forEach {
                adapter.markItemPositionSeen(it)
            }
            cacheRangeStart = firstCompletelyVisibleItem
            cacheRangeEnd = lastCompletelyVisibleItem
        }
    }
}