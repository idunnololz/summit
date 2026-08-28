package com.idunnololz.summit.notifications

import android.util.Log
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.lemmy.CommentSortType
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.inbox.InboxItem
import com.idunnololz.summit.inbox.toInboxItem
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsUpdater @AssistedInject constructor(
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val accountManager: AccountManager,
  private val apiClient: LemmyApiClient,
  private val notificationsManager: NotificationsManager,
) {
  companion object {
    private const val TAG = "NotificationsUpdater"
  }

  @AssistedFactory
  interface Factory {
    fun create(): NotificationsUpdater
  }

  private val coroutineScope = coroutineScopeFactory.create()

  fun run() {
    Log.d(TAG, "run()")

    coroutineScope.launch {
      for (account in accountManager.getAccounts()) {
        if (notificationsManager.isNotificationsEnabledForAccount(account)) {
          updateNotificationsForAccount(account)
        }
      }
    }
  }

  private suspend fun updateNotificationsForAccount(account: Account) {
    apiClient.changeInstance(account.instance)

    val j1: Deferred<List<InboxItem>> = coroutineScope.async {
      apiClient.fetchUnreadCount(force = true, account)
        .fold(
          onSuccess = {
            val jobs = mutableListOf<Deferred<List<InboxItem>>>()
            val mentions = it.mentions
            val privateMessages = it.privateMessages
            val replies = it.replies
            val commentReports = it.commentReports
            val postReports = it.postReports
            val privateMessageReports = it.privateMessageReports
            val registrationApplicationCount = it.registrationApplicationCount

            // Lemmy v1 no longer returns granular notification counts so just refresh everything
            if (mentions == null &&
              privateMessages == null &&
              replies == null &&
              it.notificationCount > 0
            ) {
              jobs.add(runMentionsJob(account))
              jobs.add(runPrivateMessagesJob(account))
              jobs.add(runRepliesJob(account))
            }
            // Lemmy v1 no longer returns granular report counts so just refresh everything
            if (commentReports == null &&
              postReports == null &&
              privateMessageReports == null &&
              it.reportCount > 0
            ) {
              jobs.add(runCommentReportsJob(account))
              jobs.add(runPostReportsJob(account))
              jobs.add(runPrivateMessageReportsJob(account))
            }

            if (mentions != null && mentions > 0) {
              jobs.add(runMentionsJob(account))
            }
            if (privateMessages != null && privateMessages > 0) {
              jobs.add(runPrivateMessagesJob(account))
            }
            if (replies != null && replies > 0) {
              jobs.add(runRepliesJob(account))
            }
            if (commentReports != null && commentReports > 0) {
              jobs.add(runCommentReportsJob(account))
            }
            if (postReports != null && postReports > 0) {
              jobs.add(runPostReportsJob(account))
            }
            if (privateMessageReports != null && privateMessageReports > 0) {
              jobs.add(runPrivateMessageReportsJob(account))
            }
            if (registrationApplicationCount > 0) {
              jobs.add(runRegistrationApplicationJob(account))
            }

            jobs.flatMap { it.await() }
          },
          onFailure = {
            Log.d(TAG, "[${account.fullName}] error updating notifications", it)
            listOf()
          },
        )
    }

    val inboxItems = mutableListOf<InboxItem>()
    inboxItems.addAll(j1.await())

    val thresholdTs = notificationsManager.getLastNotificationItemTsForAccount(account)

    val newItems = inboxItems.filter { it.lastUpdateTs > thresholdTs }
    val latestItemTs = inboxItems.maxByOrNull { it.lastUpdateTs }?.lastUpdateTs

    Log.d(
      TAG,
      "[${account.fullName}] Got ${inboxItems.size} unread content. ${newItems.size} are new!",
    )

    if (latestItemTs != null) {
      notificationsManager.setLastNotificationItemTsForAccount(account, latestItemTs)
    }

    withContext(Dispatchers.Main) {
      notificationsManager.showNotificationsForItems(account, newItems)
    }
  }

  private fun runMentionsJob(account: Account): Deferred<List<InboxItem>> = coroutineScope.async {
    apiClient.fetchMentions(
      sort = CommentSortType.New,
      page = 0,
      limit = 10,
      unreadOnly = true,
      force = true,
      account = account,
    ).fold(
      { it.map { it.toInboxItem() } },
      { listOf() },
    )
  }

  private fun runPrivateMessagesJob(account: Account): Deferred<List<InboxItem>> =
    coroutineScope.async {
      apiClient.fetchPrivateMessages(
        page = 0,
        limit = 10,
        unreadOnly = true,
        force = true,
        account = account,
      ).fold(
        { it.map { it.toInboxItem() } },
        { listOf() },
      )
    }

  private fun runRepliesJob(account: Account): Deferred<List<InboxItem>> = coroutineScope.async {
    apiClient.fetchReplies(
      sort = CommentSortType.New,
      page = 0,
      limit = 10,
      unreadOnly = true,
      force = true,
      account = account,
    ).fold(
      { it.map { it.toInboxItem() } },
      { listOf() },
    )
  }

  private fun runCommentReportsJob(account: Account): Deferred<List<InboxItem>> =
    coroutineScope.async {
      apiClient.fetchCommentReports(
        unresolvedOnly = true,
        page = 0,
        limit = 10,
        account = account,
        force = true,
      ).fold(
        { it.comment_reports.map { it.toInboxItem() } },
        { listOf() },
      )
    }

  private fun runPostReportsJob(account: Account): Deferred<List<InboxItem>> =
    coroutineScope.async {
      apiClient.fetchPostReports(
        unresolvedOnly = true,
        page = 0,
        limit = 10,
        account = account,
        force = true,
      ).fold(
        { it.post_reports.mapNotNull { it.toInboxItem() } },
        { listOf() },
      )
    }

  private fun runPrivateMessageReportsJob(account: Account): Deferred<List<InboxItem>> =
    coroutineScope.async {
      apiClient.fetchPrivateMessageReports(
        unresolvedOnly = true,
        page = 0,
        limit = 10,
        account = account,
        force = true,
      ).fold(
        {
          it.private_message_reports.map { it.toInboxItem() }
        },
        { listOf() },
      )
    }

  private fun runRegistrationApplicationJob(account: Account): Deferred<List<InboxItem>> =
    coroutineScope.async {
      apiClient.getRegistrationApplications(
        page = 0,
        limit = 10,
        unreadOnly = true,
        account = account,
        force = true,
      ).fold(
        {
          it.items.map { it.toInboxItem() }
        },
        { listOf() },
      )
    }
}
