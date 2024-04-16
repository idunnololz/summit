package com.idunnololz.summit.notifications

import android.util.Log
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.toInboxItem
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
                    {
                        val jobs = mutableListOf<Deferred<List<InboxItem>>>()

                        if (it.mentions > 0) {
                            jobs.add(runMentionsJob(account))
                        }
                        if (it.private_messages > 0) {
                            jobs.add(runPrivateMessagesJob(account))
                        }
                        if (it.replies > 0) {
                            jobs.add(runRepliesJob(account))
                        }
                        jobs.flatMap { it.await() }
                    },
                    { listOf() },
                )
        }
        val j2: Deferred<List<InboxItem>> = coroutineScope.async {
            apiClient.fetchUnresolvedReportsCount(force = true, account)
                .fold(
                    {
                        val jobs = mutableListOf<Deferred<List<InboxItem>>>()

                        if (it.comment_reports > 0) {
                            jobs.add(runCommentReportsJob(account))
                        }
                        if (it.post_reports > 0) {
                            jobs.add(runPostReportsJob(account))
                        }
                        if ((it.private_message_reports ?: 0) > 0) {
                            // todo?
                        }
                        jobs.flatMap { it.await() }
                    },
                    { listOf() },
                )
        }

        val inboxItems = mutableListOf<InboxItem>()
        inboxItems.addAll(j1.await())
        inboxItems.addAll(j2.await())

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
            page = 1,
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
                page = 1,
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
            page = 1,
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
                page = 1,
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
                page = 1,
                limit = 10,
                account = account,
                force = true,
            ).fold(
                { it.post_reports.map { it.toInboxItem() } },
                { listOf() },
            )
        }
}
