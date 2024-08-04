package com.idunnololz.summit.lemmy.inbox.repository

import android.content.Context
import android.util.Log
import com.idunnololz.summit.lemmy.inbox.LiteInboxItem
import com.idunnololz.summit.util.ext.hasInternet
import com.idunnololz.summit.util.retry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.min

class InboxSource<O>(
    @ApplicationContext context: Context,
    defaultSortOrder: O,
    private val fetchObjects: suspend (
        pageIndex: Int,
        sortOrder: O,
        limit: Int,
        force: Boolean,
    ) -> Result<List<LiteInboxItem>>,
) : LemmyListSource<LiteInboxItem, O>(
    context,
    {
        id
    },
    defaultSortOrder,
    fetchObjects,
    source = this,
) {

    fun markAsRead(id: Int, read: Boolean): LiteInboxItem? {
        val position = allObjects.indexOfFirst {
            it.obj.id == id
        }
        if (position == -1) {
            return null
        }
        val item = allObjects[position]
        val newItem = item.copy(obj = item.obj.updateIsRead(isRead = read))
        allObjects[position] = newItem

        return newItem.obj
    }

    fun removeItemWithId(id: Int): ObjectData<LiteInboxItem>? {
        val position = allObjects.indexOfFirst {
            it.obj.id == id
        }
        if (position >= 0) {
            val obj = removeItemAt(position)
            return obj
        }
        return null
    }
}

open class LemmyListSource<T, O>(
    @ApplicationContext private val context: Context,
    private val id: T.() -> Int,
    defaultSortOrder: O,
    private val fetchObjects: suspend (
        pageIndex: Int,
        sortOrder: O,
        limit: Int,
        force: Boolean,
    ) -> Result<List<T>>,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    /**
     * The source of this source. Useful if the caller needs to be able to differentiate between
     * sources or needs more information about the source.
     */
    val source: Any,
) {

    companion object {
        private const val TAG = "LemmyListSource"

        const val DEFAULT_PAGE_SIZE = 20
    }

    data class ObjectData<T>(
        val obj: T,
        val pageIndexInternal: Int,
    )

    data class PageResult<T>(
        val pageIndex: Int,
        val items: List<T>,
        val hasMore: Boolean,
    )

    protected val allObjects = mutableListOf<ObjectData<T>>()
    private val seenObjects = mutableSetOf<Int>()
    private val invalidatedPages = mutableSetOf<Int>()

    private var currentPageInternal = 1
    private var currentItemIndex = 0

    private var endReached = false

    var sortOrder: O = defaultSortOrder
        set(value) {
            if (field == value) {
                return
            }

            field = value
            invalidate()
        }

    suspend fun peekNextItem(): Result<T?> {
        return getItem(currentItemIndex, force = false)
    }

    fun next() {
        currentItemIndex++
    }

    suspend fun getItem(index: Int, force: Boolean): Result<T?> {
        if (index < allObjects.size && !force) {
            return Result.success(allObjects[index].obj)
        }
        val result = getPage(index / pageSize, force, deleteCacheOnForce = false)

        return result.fold(
            {
                Result.success(allObjects.getOrNull(index)?.obj)
            },
            {
                Result.failure(it)
            },
        )
    }

    private suspend fun getPage(
        pageIndex: Int,
        force: Boolean = false,
        deleteCacheOnForce: Boolean = true,
    ): Result<PageResult<T>> {
        Log.d(TAG, "getPage(): $pageIndex force: $force")
        val startIndex = pageIndex * pageSize
        val endIndex = startIndex + pageSize

        if (force && deleteCacheOnForce) {
            deleteCache(startIndex, endIndex)
        }

        var hasMore = true

        while (true) {
            if (allObjects.size >= endIndex) {
                break
            }

            if (endReached) {
                hasMore = false
                break
            }

            val hasMoreResult = retry {
                val finalForce = (context.hasInternet() && invalidatedPages.contains(currentPageInternal)) || force
                fetchPage(currentPageInternal, sortOrder, pageSize, finalForce)
            }

            if (hasMoreResult.isFailure) {
                return Result.failure(requireNotNull(hasMoreResult.exceptionOrNull()))
            } else {
                hasMore = hasMoreResult.getOrThrow()
                currentPageInternal++
            }

            if (!hasMore) {
                endReached = true
                break
            }
        }

        invalidatedPages.remove(pageIndex)

        return Result.success(
            PageResult(
                pageIndex,
                items = allObjects
                    .slice(startIndex until min(endIndex, allObjects.size))
                    .map { it.obj },
                hasMore = hasMore,
            ),
        )
    }

    private fun deleteCache(startIndex: Int, endIndex: Int) {
        // delete all cached data for the given page
        val minPageInteral = allObjects
            .slice(startIndex until min(endIndex, allObjects.size))
            .map { it.pageIndexInternal }
            .minOrNull() ?: 1

        deleteFromPage(minPageInteral)
        endReached = false

        Log.d(TAG, "Force = true. Clearing data. Remaining: ${allObjects.size}")
    }

    fun invalidate() {
        for (i in 1..currentPageInternal) {
            invalidatedPages.add(i)
        }
        reset()
    }

    fun reset() {
        currentPageInternal = 1
        currentItemIndex = 0

        allObjects.clear()
        seenObjects.clear()
        endReached = false
    }

    /**
     * @return true if there might be more posts to fetch
     */
    private suspend fun fetchPage(
        pageIndex: Int,
        sortOrder: O,
        limit: Int,
        force: Boolean,
    ): Result<Boolean> {
        val result = fetchObjects(pageIndex, sortOrder, limit, force)

        return result.fold(
            onSuccess = { newObjects ->
                Log.d(TAG, "Fetched ${newObjects.size} posts.")
                addObjects(newObjects, pageIndex)
                Result.success(newObjects.isNotEmpty() && newObjects.size == limit)
            },
            onFailure = {
                Result.failure(it)
            },
        )
    }

    private fun addObjects(newObjects: List<T>, pageIndex: Int) {
        newObjects.forEach {
            val id = it.id()
            if (seenObjects.add(id)) {
                allObjects.add(ObjectData(it, pageIndex))
            }
        }
    }

    private fun deleteFromPage(minPageInternal: Int) {
        allObjects.retainAll {
            val keep = it.pageIndexInternal < minPageInternal
            if (!keep) {
                seenObjects.remove(it.obj.id())
            }
            keep
        }

        currentPageInternal = minPageInternal

        Log.d(TAG, "Deleted pages $minPageInternal and beyond. Posts left: ${allObjects.size}")
    }

    protected fun removeItemAt(position: Int): ObjectData<T> {
        return allObjects.removeAt(position).also {
            seenObjects.remove(it.obj.id())
        }
    }
}
