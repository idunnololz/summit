package com.idunnololz.summit.lemmy.inbox.repository

import android.util.Log
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.util.retry
import kotlin.math.min
import kotlin.reflect.KClass

class InboxSource<O>(
    private val apiClient: AccountAwareLemmyClient,
    defaultSortOrder: O,
    private val fetchObjects: suspend AccountAwareLemmyClient.(
        pageIndex: Int, sortOrder: O, limit: Int, force: Boolean) -> Result<List<InboxItem>>
) : LemmyListSource<InboxItem, O>(
    apiClient,
    {
        id
    },
    defaultSortOrder,
    fetchObjects,
) {

    fun markAsRead(id: Int, read: Boolean): InboxItem? {
        val position = allObjects.indexOfFirst {
            it.obj.id == id
        }
        if (position == -1) {
            return null
        }
        val item = allObjects[position]
        val newItem = when (item.obj) {
            is InboxItem.MentionInboxItem ->
                item.copy(obj = item.obj.copy(isRead = read))
            is InboxItem.MessageInboxItem ->
                item.copy(obj = item.obj.copy(isRead = read))
            is InboxItem.ReplyInboxItem ->
                item.copy(obj = item.obj.copy(isRead = read))
        }
        allObjects[position] = newItem

        return newItem.obj
    }

    fun removeItemWithId(id: Int): ObjectData<InboxItem>? {
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
    private val apiClient: AccountAwareLemmyClient,
    private val id: T.() -> Int,
    defaultSortOrder: O,
    private val fetchObjects: suspend AccountAwareLemmyClient.(
        pageIndex: Int, sortOrder: O, limit: Int, force: Boolean) -> Result<List<T>>
) {

    companion object {
        private const val TAG = "InboxSource"

        private const val PAGE_SIZE = 20
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

    private var currentPageInternal = 1

    private var endReached = false

    var sortOrder: O = defaultSortOrder
        set(value) {
            field = value
            reset()
        }

    suspend fun getItem(index: Int, force: Boolean): Result<T?> {
        if (index < allObjects.size && !force) {
            return Result.success(allObjects[index].obj)
        }
        val result = getPage(index / PAGE_SIZE, force)

        return result.fold(
            {
                Result.success(allObjects.getOrNull(index)?.obj)
            },
            {
                Result.failure(it)
            }
        )
    }

    suspend fun getPage(pageIndex: Int, force: Boolean = false): Result<PageResult<T>> {
        Log.d(TAG, "getPage(): $pageIndex force: $force")
        val startIndex = pageIndex * PAGE_SIZE
        val endIndex = startIndex + PAGE_SIZE

        if (force) {
            // delete all cached data for the given page
            val minPageInteral = allObjects
                .slice(startIndex until min(endIndex, allObjects.size))
                .map { it.pageIndexInternal }
                .minOrNull() ?: 1

            deleteFromPage(minPageInteral)
            endReached = false

            Log.d(TAG, "Force = true. Clearing data. Remaining: ${allObjects.size}")
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
                fetchPage(currentPageInternal, sortOrder, PAGE_SIZE, force)
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

        return Result.success(
            PageResult(
                pageIndex,
                items = allObjects
                    .slice(startIndex until min(endIndex, allObjects.size))
                    .map { it.obj },
                hasMore = hasMore
            )
        )
    }

    fun reset() {
        currentPageInternal = 1

        allObjects.clear()
        seenObjects.clear()
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
        val result =
            apiClient.fetchObjects(pageIndex, sortOrder, limit, force)

        return result.fold(
            onSuccess = { newObjects ->
                Log.d(TAG, "Fetched ${newObjects.size} posts.")
                addObjects(newObjects, pageIndex)
                Result.success(newObjects.isNotEmpty() && newObjects.size == limit)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    private fun addObjects(newObjects: List<T>, pageIndex: Int,) {
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

        Log.d(TAG, "Deleted pages ${minPageInternal} and beyond. Posts left: ${allObjects.size}")
    }

    protected fun removeItemAt(position: Int): ObjectData<T> {
        return allObjects.removeAt(position).also {
            seenObjects.remove(it.obj.id())
        }
    }
}