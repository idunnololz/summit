package com.idunnololz.summit.lemmy.community

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Use a 2 stage strat.
 *
 * Stage 1: Mark posts as seen.
 * Stage 2: Figure out if a post is scrolled off screen.
 *
 * Only mark a post as read if seen post is scrolled off screen.
 */
class OnScrollMarkPostAsReadScrollListener(
  private val getAdapter: () -> PostListAdapter?,
  private val layoutManager: LinearLayoutManager,
) : RecyclerView.OnScrollListener() {

  private var cacheFirstVisible = -1
  private var cacheRangeStart = -1
  private var cacheRangeEnd = -1

  private var previouslyVisiblePositions = emptySet<Int>()
  private var userIsScrolling = false

  fun resetCache() {
    cacheFirstVisible = -1
    cacheRangeStart = -1
    cacheRangeEnd = -1
  }

  override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
    super.onScrollStateChanged(recyclerView, newState)

    userIsScrolling = newState == RecyclerView.SCROLL_STATE_DRAGGING ||
      newState == RecyclerView.SCROLL_STATE_SETTLING
  }

  override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
    super.onScrolled(recyclerView, dx, dy)

    if (!userIsScrolling || dy == 0) return

    val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
    val adapter = getAdapter() ?: return

    if (cacheFirstVisible == firstVisibleItem) {
      return
    }

    markItemsAsSeen(adapter, firstVisibleItem)
    calculateItemsScrolledOffScreen(adapter)
  }

  private fun calculateItemsScrolledOffScreen(adapter: PostListAdapter) {
    val first = layoutManager.findFirstVisibleItemPosition()
    val last = layoutManager.findLastVisibleItemPosition()

    if (first == RecyclerView.NO_POSITION ||
      last == RecyclerView.NO_POSITION
    ) {
      return
    }

    val currentlyVisiblePositions = (first..last)
      .toSet()

    previouslyVisiblePositions
      .minus(
        currentlyVisiblePositions,
      )
      .forEach {
        adapter.onItemScrolledOffScreen(it)
      }

    previouslyVisiblePositions = currentlyVisiblePositions
  }

  private fun markItemsAsSeen(adapter: PostListAdapter, firstVisibleItem: Int) {
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
        cacheRangeEnd == lastCompletelyVisibleItem
      ) {
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
