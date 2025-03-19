package com.idunnololz.summit.lemmy.community

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE

class CommunityScrollListener(
    private val getAdapter: () -> PostListAdapter?,
    private val layoutManager: LinearLayoutManager,
    private val viewModel: CommunityViewModel,
) : RecyclerView.OnScrollListener() {

    private var lastCachedPosition = -1

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)

        val firstPos = layoutManager.findFirstVisibleItemPosition()

        if (newState != SCROLL_STATE_IDLE) {
            return
        }

        lastCachedPosition = firstPos

        val adapter = getAdapter() ?: return
        val lastPos = layoutManager.findLastVisibleItemPosition()

        fetchPageIfLoadItem(
            adapter,
            firstPos,
            firstPos - 1,
            lastPos - 1,
            lastPos,
            lastPos + 1,
        )
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val firstPos = layoutManager.findFirstVisibleItemPosition()

        if (firstPos == lastCachedPosition) {
            return
        }

        lastCachedPosition = firstPos

        val adapter = getAdapter() ?: return
        val lastPos = layoutManager.findLastVisibleItemPosition()
        (adapter.items.getOrNull(firstPos) as? PostListEngineItem.VisiblePostItem)
            ?.pageIndex
            ?.let { pageIndex ->
                if (firstPos != 0 && lastPos == adapter.itemCount - 1) {
                    // firstPos != 0 - ensures that the page is scrollable even
                    viewModel.setPagePositionAtBottom(pageIndex)
                } else {
                    val firstView = layoutManager.findViewByPosition(firstPos)
                    viewModel.setPagePosition(
                        pageIndex,
                        firstPos,
                        firstView?.top ?: 0,
                    )
                }
            }

        if (viewModel.infinity) {
            fetchPageIfLoadItem(
                adapter,
                firstPos,
                firstPos - 1,
                lastPos - 1,
                lastPos,
                lastPos + 1,
            )
        }

        viewModel.postListEngine.updateViewingPosition(firstPos, lastPos)
    }

    fun resetCache() {
        lastCachedPosition = -1
    }

    private fun fetchPageIfLoadItem(adapter: PostListAdapter, vararg positions: Int) {
        val items = adapter.items

        for (p in positions) {
            val pageToFetch = (items.getOrNull(p) as? PostListEngineItem.AutoLoadItem)
                ?.pageToLoad
                ?: continue

            viewModel.fetchPage(pageToFetch)
        }
    }
}
