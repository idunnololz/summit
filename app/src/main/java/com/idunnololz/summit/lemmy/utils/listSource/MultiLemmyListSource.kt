package com.idunnololz.summit.lemmy.utils.listSource

import android.util.Log
import com.idunnololz.summit.inbox.repository.DEFAULT_PAGE_SIZE
import kotlin.math.min

class MultiLemmyListSource<T, O, Key>(
  private val sources: List<LemmyListSource<T, O, Key>>,
  private val pageSize: Int = DEFAULT_PAGE_SIZE,
  private val sortValue: (T) -> Long,
  private val id: (T) -> Key,
) {

  companion object {
    private const val TAG = "MultiLemmyListSource"
  }

  val seenIds = mutableSetOf<Key>()
  val allItems = mutableListOf<T>()

  suspend fun getPage(pageIndex: Int, force: Boolean, retainItemsOnForce: Boolean): PageResult<T> =
    _getPage(
      pageIndex = pageIndex,
      force = force,
      retainItemsOnForce = retainItemsOnForce,
    ).fold(
      {
        it
      },
      {
        PageResult.ErrorPageResult(pageIndex, it)
      },
    )

  private suspend fun _getPage(
    pageIndex: Int,
    force: Boolean,
    retainItemsOnForce: Boolean,
  ): Result<PageResult.SuccessPageResult<T>> {
    Log.d(
      TAG,
      "Index: $pageIndex. Sources: ${sources.size}. Force: $force",
    )

    if (force) {
      if (!retainItemsOnForce) {
        allItems.clear()
        seenIds.clear()
      }
      sources.forEach {
        it.invalidate()
      }
    }

    val startIndex = pageIndex * pageSize
    val endIndex = (pageIndex + 1) * pageSize
    var hasMore = true

    while (allItems.size < endIndex) {
      val sourceToResult = sources.map { it to it.peekNextItem() }
      val sourceAndError = sourceToResult.firstOrNull { (_, result) -> result.isFailure }

      if (sourceAndError != null) {
        return Result.failure(requireNotNull(sourceAndError.second.exceptionOrNull()))
      }

      val nextSourceAndResult = sourceToResult.maxBy { (_, result) ->
        val value = result.getOrNull() ?: return@maxBy 0L
        sortValue(value)
      }
      val nextItem = nextSourceAndResult.second.getOrNull()

      if (nextItem == null) {
        // no more items!
        hasMore = false
        break
      }

      val itemId = id(nextItem)
      Log.d(
        TAG,
        "Adding item $itemId from source ${nextItem::class.java}",
      )

      if (seenIds.add(itemId)) {
        allItems.add(nextItem)
      }

      // increment the max item
      nextSourceAndResult.first.next()
    }

    return Result.success(
      PageResult.SuccessPageResult(
        pageIndex = pageIndex,
        items = allItems
          .slice(startIndex until min(endIndex, allItems.size)),
        hasMore = hasMore,
      ),
    )
  }

  fun invalidate() {
    allItems.clear()
    seenIds.clear()
    sources.forEach {
      it.invalidate()
    }
  }
}
