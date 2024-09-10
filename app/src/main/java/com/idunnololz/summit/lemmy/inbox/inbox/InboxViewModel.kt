package com.idunnololz.summit.lemmy.inbox.inbox

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.asAccountLiveData
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.info.FullAccount
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.LiteInboxItem
import com.idunnololz.summit.lemmy.inbox.PageType
import com.idunnololz.summit.lemmy.inbox.conversation.ConversationsManager
import com.idunnololz.summit.lemmy.inbox.conversation.ConversationsModel
import com.idunnololz.summit.lemmy.inbox.repository.InboxRepository
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import com.idunnololz.summit.notifications.NotificationsManager
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
    private val inboxRepositoryFactory: InboxRepository.Factory,
    private val notificationsManager: NotificationsManager,
    private val conversationsManager: ConversationsManager,
) : ViewModel() {

    companion object {
        private const val TAG = "InboxViewModel"
    }

    private var fetchInboxJob: Job? = null

    var inboxRepository = inboxRepositoryFactory.create()

    val currentAccount
        get() = accountManager.currentAccount.asAccountLiveData()
    val currentAccountView = MutableLiveData<AccountView?>()
    val currentFullAccount = MutableLiveData<FullAccount?>()
    val markAsReadResult = StatefulLiveData<Unit>()

    val inboxUpdate = StatefulLiveData<InboxUpdate>()

    val pageTypeFlow = MutableStateFlow<PageType>(PageType.Unread)

    val pageType = pageTypeFlow.asLiveData()

    var pageIndex = 0

    var instance: String = apiClient.instance

    var pauseUnreadUpdates = false

    private var isLoaded = false
    private val allInboxItems: MutableList<InboxListItem> = mutableListOf()
    private var hasMore = true
    private val fetchingPages = mutableSetOf<Int>()
    private var conversations: ConversationsModel? = null
    private val fetchInboxRequestFlow = MutableSharedFlow<Unit>()

    val isUserOnInboxScreen = MutableStateFlow<Boolean>(false)
    val lastInboxUnreadLoadTimeMs = MutableStateFlow<Long>(0)
    var clearInboxNotificationsJob: Job? = null
    var observeConversationsJob: Job? = null

    init {
        viewModelScope.launch {
            accountManager.currentAccountOnChange.collect {
                val account = it as? Account
                if (account != null) {
                    instance = account.instance
                }

                inboxRepository = inboxRepositoryFactory.create()

                delay(10) // just in case it takes a second for the api client to update...
                pageIndex = 0

                clearData()

                inboxUpdate.setValue(
                    InboxUpdate(
                        inboxModel = InboxModel(),
                        scrollToTop = false,
                    ),
                )

                fetchInboxAsync()
            }
        }

        viewModelScope.launch {
            accountManager.currentAccount.collect {
                withContext(Dispatchers.Main) {
                    val account = it as? Account
                    if (account != null) {
                        currentAccountView.value = accountInfoManager.getAccountViewForAccount(account)
                    } else {
                        currentAccountView.value = null
                    }
                }
            }
        }

        viewModelScope.launch {
            pageTypeFlow.collect {
                withContext(Dispatchers.Main) {
                    pageIndex = 0

                    clearData()

                    inboxUpdate.setValue(
                        InboxUpdate(
                            inboxModel = InboxModel(),
                            scrollToTop = false,
                        ),
                    )

                    fetchInbox()
                }
            }
        }

        viewModelScope.launch {
            accountInfoManager.unreadCount.collect {
                if (!pauseUnreadUpdates) {
                    inboxRepository.onServerChanged()
                }
            }
        }
        viewModelScope.launch {
            accountInfoManager.currentFullAccount.collect {
                currentFullAccount.value = it
            }
        }

        viewModelScope.launch {
            isUserOnInboxScreen.collect {
                checkIfDismissInboxNotifications()
            }
        }
        viewModelScope.launch {
            lastInboxUnreadLoadTimeMs.collect {
                checkIfDismissInboxNotifications()
            }
        }
        viewModelScope.launch {
            fetchInboxRequestFlow.debounce(100)
                .collect {
                    fetchInbox()
                }
        }
        inboxRepository.onServerChanged()
    }

    fun fetchInboxAsync() {
        viewModelScope.launch {
            fetchInboxRequestFlow.emit(Unit)
        }
    }

    fun fetchInbox(pageIndex: Int = this.pageIndex, force: Boolean = false) {
        val pageType = pageTypeFlow.value

        observeConversationsJob?.cancel()

        if (pageType == PageType.Messages) {
            fetchInboxJob?.cancel()
            fetchConversations()
            return
        }

        if (force) {
            if (fetchingPages.contains(pageIndex)) {
                fetchingPages.remove(pageIndex)
            }
        } else if (fetchingPages.contains(pageIndex)) {
            return
        }

        fetchingPages.add(pageIndex)
        fetchInboxJob?.cancel()
        Log.d(TAG, "Loading inbox page - " +
            "pageIndex: $pageIndex pageType: $pageType force: $force")

        inboxUpdate.setIsLoading()
        fetchInboxJob = viewModelScope.launch {
            val result = inboxRepository.getPage(pageIndex, pageType, force)

            ensureActive()

            result
                .onSuccess {
                    val pageResult =
                        it.copy(
                            items = it.items
                                .filter {
                                    if (it is InboxItem.MessageInboxItem) {
                                        it.authorId != currentAccount.value?.id
                                    } else {
                                        true
                                    }
                                },
                        )

                    addData(pageResult.toInboxItemResult())

                    publishInboxUpdate(scrollToTop = pageIndex == 0 && force)

                    withContext(Dispatchers.Main) {
                        fetchingPages.clear()
                        isLoaded = true
                    }
                }
                .onFailure {
                    inboxUpdate.postError(it)

                    withContext(Dispatchers.Main) {
                        fetchingPages.clear()
                        isLoaded = true
                    }
                }
        }
    }

    fun markAsRead(inboxItem: InboxItem, read: Boolean, refreshAfter: Boolean = false) {
        markAsReadResult.setIsLoading()

        markAsReadInViewData(
            id = inboxItem.id.toLong(),
            isRead = read,
        )

        viewModelScope.launch {
            val currentAccount = currentAccount.value
            if (currentAccount != null) {
                notificationsManager.removeNotificationForInboxItem(inboxItem, currentAccount)
            }

            inboxRepository.markAsRead(inboxItem, read)
                .onSuccess {
                    if (!read || refreshAfter) {
                        fetchInbox(force = true)
                    }
                    markAsReadResult.postValue(Unit)
                }
                .onFailure {
                    fetchInbox(force = true)
                    markAsReadResult.postError(it)
                }
        }
    }

    fun markAllAsRead() {
        markAsReadResult.setIsLoading()
        viewModelScope.launch {
            apiClient.markAllAsRead()
                .onSuccess {
                    fetchInbox(force = true)
                }
                .onFailure {
                    fetchInbox(force = true)
                }
        }
    }

    private fun publishInboxUpdate(scrollToTop: Boolean) {
        val conversations = conversations

        if (conversations != null) {
            inboxUpdate.postValue(
                InboxUpdate(
                    inboxModel = InboxModel(
                        conversations.conversations.map {
                            InboxListItem.ConversationItem(
                                conversation = it,
                                draftMessage = conversations.drafts[it.personId]?.draftData,
                            )
                        },
                        earliestMessageTs = conversations.conversationEarliestMessageTs,
                        hasMore = false,
                    ),
                    scrollToTop = scrollToTop,
                ),
            )
        } else {
            inboxUpdate.postValue(
                InboxUpdate(
                    inboxModel = InboxModel(
                        allInboxItems,
                        hasMore = hasMore,
                    ),
                    scrollToTop = scrollToTop,
                ),
            )
        }
    }

    private fun fetchConversations() {
        inboxUpdate.setIsLoading()

        observeConversationsJob?.cancel()
        observeConversationsJob = viewModelScope.launch(Dispatchers.Default) {
            conversationsManager.conversationsFlow.collect {
                if (it.isLoaded) {
                    conversations = it
                    publishInboxUpdate(scrollToTop = false)
                } else {
                    inboxUpdate.postIsLoading()
                }
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            conversationsManager.refreshConversations()
                .onSuccess {
                    // handled by updateConversationsJob
                }
                .onFailure {
                    inboxUpdate.postError(it)
                }
        }
    }

    private fun markAsReadInViewData(id: Long, isRead: Boolean) {
        for ((index, data) in allInboxItems.withIndex()) {
            when (data) {
                is InboxListItem.ConversationItem -> {
                    if (data.conversation.mostRecentMessageId != id) {
                        continue
                    }
                    allInboxItems[index] = data.copy(conversation = data.conversation.copy(isRead = isRead))
                }
                is InboxListItem.RegularInboxItem -> {
                    if (data.item.id.toLong() != id) {
                        continue
                    }
                    allInboxItems[index] = data.copy(item = data.item.updateIsRead(isRead = isRead) as InboxItem)
                }
            }
        }

        if (isLoaded) {
            publishInboxUpdate(scrollToTop = false)
        }
    }

    private fun addData(data: LemmyListSource.PageResult<InboxItem>) {
        if (data.pageIndex == 0) {
            clearData()
        }

        hasMore = data.hasMore

        allInboxItems.addAll(data.items.map { InboxListItem.RegularInboxItem(it) })

        Log.d(TAG, "Data updated! Total items: ${allInboxItems.size} hasMore: $hasMore")
    }

    private fun clearData() {
        hasMore = true
        isLoaded = false
        allInboxItems.clear()
        fetchingPages.clear()
        conversations = null
    }

    override fun onCleared() {
        fetchInboxJob?.cancel()
        super.onCleared()
    }

    private fun checkIfDismissInboxNotifications() {
        val inboxStaleTimeMs = System.currentTimeMillis() - lastInboxUnreadLoadTimeMs.value
        if (isUserOnInboxScreen.value && inboxStaleTimeMs < 10_000) {
            clearInboxNotificationsJob = viewModelScope.launch {
                delay(1_000)

                Log.d(TAG, "User has been on inbox screen for long enough. Clearing inbox notifications.")
                clearInboxNotifications()
            }
        } else {
            clearInboxNotificationsJob?.cancel()
        }
    }

    fun clearInboxNotifications() {
        currentAccount.value?.let {
            notificationsManager.removeAllInboxNotificationsForAccount(it)
        }
    }
}

private fun LemmyListSource.PageResult<LiteInboxItem>.toInboxItemResult(): LemmyListSource.PageResult<InboxItem> =
    LemmyListSource.PageResult(
        pageIndex = pageIndex,
        items = items.filterIsInstance<InboxItem>(),
        hasMore = hasMore,
    )

fun PageType.getName(context: Context) = when (this) {
    PageType.Unread -> context.getString(R.string.unread)
    PageType.All -> context.getString(R.string.all)
    PageType.Replies -> context.getString(R.string.replies)
    PageType.Mentions -> context.getString(R.string.mentions)
    PageType.Messages -> context.getString(R.string.messages)
    PageType.Reports -> context.getString(R.string.reports)
    PageType.Conversation -> context.getString(R.string.messages)
}

data class InboxUpdate(
    val inboxModel: InboxModel,
    val scrollToTop: Boolean,
)
