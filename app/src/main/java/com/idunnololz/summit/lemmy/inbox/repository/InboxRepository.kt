package com.idunnololz.summit.lemmy.inbox.repository

import android.content.Context
import android.util.Log
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.info.FullAccount
import com.idunnololz.summit.account.info.isAdmin
import com.idunnololz.summit.account.info.isMod
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.NotAModOrAdmin
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.LiteInboxItem
import com.idunnololz.summit.lemmy.inbox.PageType
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource.PageResult
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

@ViewModelScoped
class InboxRepository @Inject constructor(
  @ApplicationContext private val context: Context,
  private val apiClient: AccountAwareLemmyClient,
  private val accountInfoManager: AccountInfoManager,
  private val fullAccount: FullAccount?,
  private val conversationSource: InboxMultiDataSource,
) {

  @ViewModelScoped
  class Factory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: AccountAwareLemmyClient,
    private val accountInfoManager: AccountInfoManager,
  ) {
    fun create(
      fullAccount: FullAccount?,
      conversationSource: InboxMultiDataSource = InboxMultiDataSource(listOf()),
    ) = InboxRepository(
      context = context,
      apiClient = apiClient,
      accountInfoManager = accountInfoManager,
      fullAccount = fullAccount,
      conversationSource = conversationSource,
    )
  }

  companion object {
    private const val TAG = "InboxRepository"
  }

  private val repliesStatelessSource: InboxSource<CommentSortType> =
    makeRepliesSource(unreadOnly = false)
  private val mentionsStatelessSource: InboxSource<CommentSortType> =
    makeMentionsSource(unreadOnly = false)
  private val messagesStatelessSource: InboxSource<Unit> =
    makeMessagesSource(unreadOnly = false)
  private val postReportsStatelessSource: InboxSource<Unit> =
    makePostReportsSource(unresolvedOnly = false)
  private val commentReportsStatelessSource: InboxSource<Unit> =
    makeCommentReportsSource(unresolvedOnly = false)
  private val makePrivateMessageReportsSource: InboxSource<Unit> =
    makePrivateMessageReportsSource(unresolvedOnly = false)
  private val registrationApplicationsStatelessSource: InboxSource<Unit> =
    makeRegistrationApplicationsSource(unreadOnly = false)

  class InboxMultiDataSource(
    private val sources: List<InboxSource<*>>,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
  ) {

    val seenIds = mutableSetOf<Int>()
    val allItems = mutableListOf<LiteInboxItem>()

    suspend fun getPage(
      pageIndex: Int,
      force: Boolean,
      retainItemsOnForce: Boolean,
    ): Result<PageResult<LiteInboxItem>> = withContext(Dispatchers.Default) {
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
        ensureActive()

        val sourceToResult = sources.map { it to it.peekNextItem() }
        val sourceAndError = sourceToResult.firstOrNull { (_, result) -> result.isFailure }

        if (sourceAndError != null) {
          return@withContext Result.failure(requireNotNull(sourceAndError.second.exceptionOrNull()))
        }

        if (sourceToResult.isEmpty()) {
          return@withContext Result.failure(RuntimeException("No sources!"))
        }

        val nextSourceAndResult = sourceToResult.maxBy {
            (_, result) ->
          result.getOrNull()?.lastUpdateTs ?: 0L
        }
        val nextItem = nextSourceAndResult.second.getOrNull()

        if (nextItem == null) {
          // no more items!
          hasMore = false
          break
        }

        Log.d(
          TAG,
          "Adding item ${nextItem.id} from source ${nextItem::class.java}",
        )

        if (seenIds.add(nextItem.id)) {
          allItems.add(nextItem)
        }

        // increment the max item
        nextSourceAndResult.first.next()
      }

      Result.success(
        PageResult(
          pageIndex,
          allItems
            .slice(startIndex until min(endIndex, allItems.size)),
          hasMore = hasMore,
        ),
      )
    }

    fun markAsRead(id: Int, read: Boolean): LiteInboxItem? {
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
      seenIds.clear()
      sources.forEach {
        it.invalidate()
      }
    }
  }

  private val repliesSource = InboxMultiDataSource(
    listOf(repliesStatelessSource),
  )
  private val mentionsSource =
    InboxMultiDataSource(
      listOf(mentionsStatelessSource),
    )
  private val messagesSource =
    InboxMultiDataSource(
      listOf(messagesStatelessSource),
    )
  private val reportsSource =
    InboxMultiDataSource(
      mutableListOf<InboxSource<*>>().apply {
        if (fullAccount?.isMod() == true) {
          add(postReportsStatelessSource)
          add(commentReportsStatelessSource)
        }
        if (fullAccount?.isAdmin() == true) {
          add(makePrivateMessageReportsSource)
        }
      },
    )
  private val allSources = InboxMultiDataSource(
    listOf(
      repliesStatelessSource,
      mentionsStatelessSource,
      messagesStatelessSource,
      postReportsStatelessSource,
      commentReportsStatelessSource,
    ),
  )
  private val unreadSources = InboxMultiDataSource(
    mutableListOf<InboxSource<*>>().apply {
      add(makeRepliesSource(unreadOnly = true))
      add(makeMentionsSource(unreadOnly = true))
      add(makeMessagesSource(unreadOnly = true))

      if (fullAccount?.isMod() == true) {
        add(makePostReportsSource(unresolvedOnly = true))
        add(makeCommentReportsSource(unresolvedOnly = true))
      }
      if (fullAccount?.isAdmin() == true) {
        add(makePrivateMessageReportsSource(unresolvedOnly = true))
      }
    },
  )
  private val registrationApplicationsSource =
    InboxMultiDataSource(
      listOf(registrationApplicationsStatelessSource),
    )

  private fun getSource(pageType: PageType) = when (pageType) {
    PageType.Unread -> unreadSources
    PageType.All -> allSources
    PageType.Replies -> repliesSource
    PageType.Mentions -> mentionsSource
    PageType.Messages -> messagesSource
    PageType.Reports -> reportsSource
    PageType.Conversation -> conversationSource
    PageType.Applications -> registrationApplicationsSource
  }

  suspend fun getPage(
    pageIndex: Int,
    pageType: PageType,
    force: Boolean,
    retainItemsOnForce: Boolean = false,
  ): Result<PageResult<LiteInboxItem>> {
    val source = getSource(pageType)

    val result = source.getPage(pageIndex, force, retainItemsOnForce)

    Log.d(TAG, "Got ${result.getOrNull()?.items?.size} items for page $pageType")

    return result
  }

  fun invalidate(pageType: PageType) {
    getSource(pageType).invalidate()
  }

  private fun makeRepliesSource(unreadOnly: Boolean) = InboxSource(
    context,
    CommentSortType.New,
  ) { page: Int, sortOrder: CommentSortType, limit: Int, force: Boolean ->
    apiClient.fetchReplies(
      sort = sortOrder,
      page = page,
      limit = limit,
      unreadOnly = unreadOnly,
      force = force,
    ).fold(
      onSuccess = {
        Result.success(it.map { InboxItem.ReplyInboxItem(it) })
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  private fun makeMentionsSource(unreadOnly: Boolean) = InboxSource(
    context,
    CommentSortType.New,
  ) { page: Int, sortOrder: CommentSortType, limit: Int, force: Boolean ->
    apiClient.fetchMentions(
      sort = sortOrder,
      page = page,
      limit = limit,
      unreadOnly = unreadOnly,
      force = force,
    ).fold(
      onSuccess = {
        Result.success(it.map { InboxItem.MentionInboxItem(it) })
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  private fun makeMessagesSource(unreadOnly: Boolean) = InboxSource(
    context,
    Unit,
  ) { page: Int, _: Unit, limit: Int, force: Boolean ->
    apiClient.fetchPrivateMessages(
      page = page,
      limit = limit,
      unreadOnly = unreadOnly,
      force = force,
    ).fold(
      onSuccess = {
        Result.success(
          it.map { InboxItem.MessageInboxItem(it) },
        )
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  private fun makePostReportsSource(unresolvedOnly: Boolean) = InboxSource(
    context,
    Unit,
  ) { page: Int, _: Unit, limit: Int, force: Boolean ->
    apiClient.fetchPostReports(
      page = page,
      limit = limit,
      unresolvedOnly = unresolvedOnly,
      force = force,
    ).fold(
      onSuccess = {
        Result.success(it.post_reports.map { InboxItem.ReportPostInboxItem(it) })
      },
      onFailure = {
        if (it is NotAModOrAdmin) {
          Result.success(listOf())
        } else {
          Result.failure(it)
        }
      },
    )
  }

  private fun makeCommentReportsSource(unresolvedOnly: Boolean) = InboxSource(
    context,
    Unit,
  ) { page: Int, _: Unit, limit: Int, force: Boolean ->
    apiClient.fetchCommentReports(
      page = page,
      limit = limit,
      unresolvedOnly = unresolvedOnly,
      force = force,
    ).fold(
      onSuccess = {
        Result.success(it.comment_reports.map { InboxItem.ReportCommentInboxItem(it) })
      },
      onFailure = {
        if (it is NotAModOrAdmin) {
          Result.success(listOf())
        } else {
          Result.failure(it)
        }
      },
    )
  }

  private fun makePrivateMessageReportsSource(unresolvedOnly: Boolean) = InboxSource(
    context,
    Unit,
  ) { page: Int, _: Unit, limit: Int, force: Boolean ->
    apiClient.fetchPrivateMessageReports(
      page = page,
      limit = limit,
      unresolvedOnly = unresolvedOnly,
      force = force,
    ).fold(
      onSuccess = {
        Result.success(it.private_message_reports.map { InboxItem.ReportMessageInboxItem(it) })
      },
      onFailure = {
        if (it is NotAModOrAdmin) {
          Result.success(listOf())
        } else {
          Result.failure(it)
        }
      },
    )
  }

  private fun makeRegistrationApplicationsSource(unreadOnly: Boolean) = InboxSource(
    context,
    Unit,
  ) { page: Int, _: Unit, limit: Int, force: Boolean ->
    apiClient.getRegistrationApplications(
      page = page,
      limit = limit,
      unreadOnly = unreadOnly,
      force = force,
    ).fold(
      onSuccess = {
        Result.success(
          it.registration_applications.map { InboxItem.RegistrationApplicationInboxItem(it) },
        )
      },
      onFailure = {
        if (it is NotAModOrAdmin) {
          Result.success(listOf())
        } else {
          Result.failure(it)
        }
      },
    )
  }

  suspend fun markAsRead(inboxItem: InboxItem, read: Boolean): Result<Unit> {
    val itemMarked = allSources.markAsRead(inboxItem.id, read)

    if (read) {
      unreadSources.removeItemWithId(inboxItem.id)
    }

    allSources.invalidate()
    unreadSources.invalidate()

    invalidate(PageType.Unread)
    val result = when (inboxItem) {
      is InboxItem.MentionInboxItem ->
        apiClient.markMentionAsRead(inboxItem.id, read)
      is InboxItem.MessageInboxItem ->
        apiClient.markPrivateMessageAsRead(inboxItem.id, read)
      is InboxItem.ReplyInboxItem ->
        apiClient.markReplyAsRead(inboxItem.id, read)
      is InboxItem.ReportMessageInboxItem -> {
        apiClient.resolvePrivateMessageReport(inboxItem.id, read)
      }
      is InboxItem.ReportCommentInboxItem -> {
        apiClient.resolveCommentReport(inboxItem.id, read)
      }
      is InboxItem.ReportPostInboxItem -> {
        apiClient.resolvePostReport(inboxItem.id, read)
      }
      is InboxItem.RegistrationApplicationInboxItem -> {
        Result.success(Unit)
      }
    }

    return result.fold(
      onSuccess = {
        getPage(0, PageType.Unread, force = true, retainItemsOnForce = true)
        invalidate(PageType.Unread)

        accountInfoManager.updateUnreadCount()

        Result.success(Unit)
      },
      onFailure = {
        allSources.markAsRead(inboxItem.id, !read)

        if (read) {
          getPage(0, PageType.Unread, force = true, retainItemsOnForce = true)
          invalidate(PageType.Unread)
        }
        Result.failure(it)
      },
    )
  }

  fun onServerChanged() {
    PageType.entries.forEach {
      invalidate(it)
    }
  }
}
