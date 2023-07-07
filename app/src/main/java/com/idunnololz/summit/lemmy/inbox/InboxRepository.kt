package com.idunnololz.summit.lemmy.inbox

import android.util.Log
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.lemmy.inbox.repository.InboxSource
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource.PageResult
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlin.math.min

@ViewModelScoped
class InboxRepository @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountInfoManager: AccountInfoManager,
) {

    @ViewModelScoped
    class Factory @Inject constructor(
        private val apiClient: AccountAwareLemmyClient,
        private val accountInfoManager: AccountInfoManager,
    ) {
        fun create() =
            InboxRepository(apiClient, accountInfoManager)
    }

    companion object {
        private const val TAG = "InboxRepository"

        private const val PAGE_SIZE = 20
    }

    private val repliesStatelessSource: InboxSource<CommentSortType> =
        makeRepliesSource(unreadOnly = false)
    private val mentionsStatelessSource: InboxSource<CommentSortType> =
        makeMentionsSource(unreadOnly = false)
    private val messagesStatelessSource: InboxSource<Unit> =
        makeMessagesSource(unreadOnly = false)

    private class InboxSourceState<O>(
        val inboxSource: InboxSource<O>,
        var itemIndex: Int = 0,
    ) {
        private var nextItem: Result<InboxItem?>? = null
        suspend fun getItem(force: Boolean): Result<InboxItem?> {
            nextItem?.let {
                return it
            }

            val result = inboxSource.getItem(itemIndex, force)
            Log.d(TAG, "Setting next item to ${(result.getOrNull() ?: Unit)::class.java}. Index: $itemIndex")

            val nextItem = result.also {
                nextItem = it
            }
            return nextItem
        }

        suspend fun next() {
            if (nextItem?.getOrNull() != null) {
                itemIndex++
                nextItem = null
            }
        }

        fun reset() {
            nextItem = null
            itemIndex = 0
        }

        fun markAsRead(id: Int, read: Boolean): InboxItem? {
            return inboxSource.markAsRead(id, read)
        }

        fun removeItemWithId(id: Int) =
            inboxSource.removeItemWithId(id)
    }

    private class InboxMultiDataSource(
        private val sources: List<InboxSourceState<*>>
    ) {

        val allItems = mutableListOf<InboxItem>()

        suspend fun getPage(
            pageIndex: Int,
            pageType: InboxViewModel.PageType,
            force: Boolean,
        ): Result<PageResult<InboxItem>> {

            Log.d(TAG,
                "Page type: $pageType. Index: $pageIndex. Sources: ${sources.size}. Force: $force")

            if (force) {
                allItems.clear()
                sources.forEach { it.reset() }
            }

            val startIndex = pageIndex * PAGE_SIZE
            val endIndex = (pageIndex + 1) * PAGE_SIZE
            var hasMore = true

            while (allItems.size < endIndex) {
                val results = sources.map { it.getItem(force) }
                val error = results.firstOrNull { it.isFailure }

                if (error != null) {
                    return Result.failure(requireNotNull(error.exceptionOrNull()))
                }

                val nextSource = sources.maxBy { it.getItem(force).getOrNull()?.lastUpdateTs ?: 0L }
                val nextItem = nextSource.getItem(force).getOrNull()

                if (nextItem == null) {
                    // no more items!
                    hasMore = false
                    break
                }

                Log.d(TAG,
                    "Adding item ${nextItem.id} from source ${nextItem::class.java}")

                allItems.add(nextItem)

                // increment the max item
                nextSource.next()
            }

            return Result.success(
                PageResult(
                    pageIndex,
                    allItems
                        .slice(startIndex until min(endIndex, allItems.size)),
                    hasMore = hasMore,
                )
            )
        }

        fun markAsRead(id: Int, read: Boolean): InboxItem? {
            sources.forEach {
                val item = it.markAsRead(id, read)
                if (item != null) {
                    return item
                }
            }
            return null
        }

        fun removeItemWithId(id: Int) {
            sources.forEach {
                it.removeItemWithId(id)
            }
        }

        fun invalidate() {
            allItems.clear()
            sources.forEach {
                it.reset()
            }
        }
    }

    private val repliesSource = InboxMultiDataSource(
        listOf(InboxSourceState(repliesStatelessSource))
    )
    private val mentionsSource =
        InboxMultiDataSource(
            listOf(InboxSourceState(mentionsStatelessSource))
        )
    private val messagesSource =
        InboxMultiDataSource(
            listOf(InboxSourceState(messagesStatelessSource))
        )
    private val allSources = InboxMultiDataSource(
        listOf(
            InboxSourceState(repliesStatelessSource),
            InboxSourceState(mentionsStatelessSource),
            InboxSourceState(messagesStatelessSource)
        )
    )
    private val unreadSources = InboxMultiDataSource(
        listOf(
            InboxSourceState(makeRepliesSource(unreadOnly = true)),
            InboxSourceState(makeMentionsSource(unreadOnly = true)),
            InboxSourceState(makeMessagesSource(unreadOnly = true))
        )
    )

    suspend fun getPage(
        pageIndex: Int,
        pageType: InboxViewModel.PageType,
        force: Boolean,
    ): Result<PageResult<InboxItem>> {

        val source = when (pageType) {
            InboxViewModel.PageType.Unread -> unreadSources
            InboxViewModel.PageType.All -> allSources
            InboxViewModel.PageType.Replies -> repliesSource
            InboxViewModel.PageType.Mentions -> mentionsSource
            InboxViewModel.PageType.Messages -> messagesSource
        }

        return source.getPage(pageIndex, pageType, force)
    }

    suspend fun invalidate(pageType: InboxViewModel.PageType) {
        val source = when (pageType) {
            InboxViewModel.PageType.Unread -> unreadSources
            InboxViewModel.PageType.All -> allSources
            InboxViewModel.PageType.Replies -> repliesSource
            InboxViewModel.PageType.Mentions -> mentionsSource
            InboxViewModel.PageType.Messages -> messagesSource
        }
        source.invalidate()
    }

    private fun makeRepliesSource(unreadOnly: Boolean) = InboxSource(
        apiClient,
        CommentSortType.New
    ) { page: Int, sortOrder: CommentSortType, limit: Int, force: Boolean ->
        fetchReplies(
            sort = sortOrder,
            page = page,
            limit = limit,
            unreadOnly = unreadOnly,
            force = force
        ).fold(
            onSuccess = {
                Result.success(it.map { InboxItem.ReplyInboxItem(it) })
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    private fun makeMentionsSource(unreadOnly: Boolean) = InboxSource(
        apiClient,
        CommentSortType.New
    ) { page: Int, sortOrder: CommentSortType, limit: Int, force: Boolean ->
        fetchMentions(
            sort = sortOrder,
            page = page,
            limit = limit,
            unreadOnly = unreadOnly,
            force = force
        ).fold(
            onSuccess = {
                Result.success(it.map { InboxItem.MentionInboxItem(it) })
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    private fun makeMessagesSource(unreadOnly: Boolean) = InboxSource(
        apiClient,
        Unit
    ) { page: Int, _: Unit, limit: Int, force: Boolean ->
        fetchPrivateMessages(
            page = page,
            limit = limit,
            unreadOnly = unreadOnly,
            force = force
        ).fold(
            onSuccess = {
                Result.success(it.map { InboxItem.MessageInboxItem(it) })
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun markAsRead(inboxItem: InboxItem, read: Boolean): Result<Unit> {
        val itemMarked = allSources.markAsRead(inboxItem.id, read)

        if (read) {
            unreadSources.removeItemWithId(inboxItem.id)
        }

        allSources.invalidate()
        unreadSources.invalidate()

        invalidate(InboxViewModel.PageType.Unread)
        val result = when (inboxItem) {
            is InboxItem.MentionInboxItem ->
                apiClient.markMentionAsRead(inboxItem.id, read)
            is InboxItem.MessageInboxItem ->
                apiClient.markPrivateMessageAsRead(inboxItem.id, read)
            is InboxItem.ReplyInboxItem ->
                apiClient.markReplyAsRead(inboxItem.id, read)
        }

        return result.fold(
            onSuccess = {
                getPage(0, InboxViewModel.PageType.Unread, force = true)
                invalidate(InboxViewModel.PageType.Unread)

                accountInfoManager.updateUnreadCount()

                Result.success(Unit)
            },
            onFailure = {
                allSources.markAsRead(inboxItem.id, !read)

                if (read) {
                    getPage(0, InboxViewModel.PageType.Unread, force = true)
                    invalidate(InboxViewModel.PageType.Unread)
                }
                Result.failure(it)
            }
        )
    }
}

