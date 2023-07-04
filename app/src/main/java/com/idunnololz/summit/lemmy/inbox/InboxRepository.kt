package com.idunnololz.summit.lemmy.inbox

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommentReplyView
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.PostsRepository
import com.idunnololz.summit.lemmy.inbox.InboxSource.PageResult
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.util.retry
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlin.math.min

@ViewModelScoped
class InboxRepository @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
) {
    val repliesSource = InboxSource(
        apiClient,
        {
            comment.id
        },
        CommentSortType.New,
        { page: Int, sortOrder: CommentSortType, force: Boolean ->
            fetchReplies(sort = sortOrder, page = page, force = force)
        }
    )
    val mentionsSource = InboxSource(
        apiClient,
        {
            comment.id
        },
        CommentSortType.New,
        { page: Int, sortOrder: CommentSortType, force: Boolean ->
            fetchMentions(sort = sortOrder, page = page, force = force)
        }
    )
    val messagesSource = InboxSource(
        apiClient,
        {
            private_message.id
        },
        Unit,
        { page: Int, _: Unit, force: Boolean ->
            fetchPrivateMessages(page = page, force = force)
        }
    )
}



class InboxSource<T, O>(
    private val apiClient: AccountAwareLemmyClient,
    private val id: T.() -> Int,
    defaultSortOrder: O,
    private val fetchObjects: suspend AccountAwareLemmyClient.(
        pageIndex: Int, sortOrder: O, force: Boolean) -> Result<List<T>>
) {

    companion object {
        private const val TAG = "InboxSource"

        private const val PAGE_SIZE = 20
    }


    private data class ObjectData<T>(
        val obj: T,
        val pageIndexInternal: Int,
    )

    class PageResult<T>(
        val items: List<T>,
        val hasMore: Boolean,
    )

    private val allObjects = mutableListOf<ObjectData<T>>()
    private val seenObjects = mutableSetOf<Int>()

    private var currentPageInternal = 1

    private var endReached = false

    var sortOrder: O = defaultSortOrder
        set(value) {
            field = value
            reset()
        }

    suspend fun getPage(pageIndex: Int, force: Boolean = false): Result<PageResult<T>> {
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
                fetchPage(currentPageInternal, sortOrder, force)
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
        force: Boolean,
    ): Result<Boolean> {
        val result =
            apiClient.fetchObjects(pageIndex, sortOrder, force)

        return result.fold(
            onSuccess = { newObjects ->
                Log.d(TAG, "Fetched ${newObjects.size} posts.")
                addObjects(newObjects, pageIndex)
                Result.success(newObjects.isNotEmpty())
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
}