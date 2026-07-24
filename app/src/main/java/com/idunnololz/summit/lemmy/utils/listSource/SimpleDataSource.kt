package com.idunnololz.summit.lemmy.utils.listSource

import android.util.Log
import com.idunnololz.summit.lemmy.utils.listSource.LemmyListSource.Companion.DEFAULT_PAGE_SIZE
import kotlin.map
import kotlin.onSuccess

interface SimpleDataSource<T, O> {
  suspend fun fetchItems(
    sortOrder: O? = null,
    page: Int,
    force: Boolean,
    limit: Int = DEFAULT_PAGE_SIZE,
    showRead: Boolean?,
  ): Result<List<T>>
}

class Page<T>(
  val items: List<T>,
  val nextCursor: String?,
)

abstract class CursorBackedSingleDataSource<T, O>(
  private val fetchObjects: suspend (
    pageCursor: String?,
    sortOrder: O?,
    limit: Int,
    force: Boolean,
    showRead: Boolean?,
  ) -> Result<Page<T>>,
) : SimpleDataSource<T, O> {

  companion object {
    private const val TAG = "CursorBackedSingleDataSource"
  }

  private var cursorIds = listOf<String?>("0")

  override suspend fun fetchItems(
    sortOrder: O?,
    page: Int,
    force: Boolean,
    limit: Int,
    showRead: Boolean?,
  ): Result<List<T>> {
    Log.d(TAG, "fetchPosts - page: $page cursors: ${cursorIds.size}")

    buildCursorIds(
      sortOrder = sortOrder,
      page = page,
      force = force,
      limit = limit,
      showRead = showRead,
    ).onFailure {
      return Result.failure(it)
    }

    return _fetchPosts(
      sortOrder = sortOrder,
      page = page,
      force = force,
      limit = limit,
      showRead = showRead,
    )
      .onSuccess {
        if (page + 1 == cursorIds.size) {
          cursorIds += it.nextCursor
        } else {
          if (cursorIds[page + 1] != it.nextCursor) {
            Log.d(TAG, "inconsistency cursor ids... wiping cached cursor ids")

            cursorIds = cursorIds.take(page + 1)
            cursorIds += it.nextCursor
          }
        }
      }
      .map {
        it.items
      }
  }

  private suspend fun buildCursorIds(
    sortOrder: O?,
    page: Int,
    force: Boolean,
    limit: Int,
    showRead: Boolean?,
  ): Result<Unit> {
    if (cursorIds.size > page) {
      return Result.success(Unit)
    }

    Log.d(TAG, "cursor index outside of current range, fetching more cursors...")

    while (cursorIds.size <= page) {
      _fetchPosts(
        sortOrder = sortOrder,
        page = cursorIds.lastIndex,
        force = force,
        limit = limit,
        showRead = showRead,
      ).fold(
        {
          cursorIds += it.nextCursor
        },
        {
          return Result.failure(it)
        },
      )
    }

    return Result.success(Unit)
  }

  private suspend fun _fetchPosts(
    sortOrder: O?,
    page: Int,
    force: Boolean,
    limit: Int,
    showRead: Boolean?,
  ): Result<Page<T>> {
    if (page > 0 && cursorIds[page] == null) {
      return Result.success(
        Page(listOf(), null),
      )
    }

    val pageIndex = if (page == 0) {
      page
    } else {
      null
    }
    val cursor = if (page == 0) {
      null
    } else {
      cursorIds[page]
    }

    Log.d(TAG, "_fetchPosts - $pageIndex $cursor")

    return fetchObjects(
      cursor,
      sortOrder,
      limit,
      force,
      showRead,
    )
  }
}
