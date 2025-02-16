package com.idunnololz.summit.lemmy.inbox.inbox

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
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
    private val savedStateHandle: SavedStateHandle,
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

    val pageTypeFlow = savedStateHandle.getStateFlow("page_type", PageType.Unread)

    val pageType = pageTypeFlow.asLiveData()

    var instance: String = apiClient.instance

    var pauseUnreadUpdates = false

    private var isLoaded = false
    private val allInboxItems: MutableList<InboxListItem> = mutableListOf()
    private var hasMore = true
    private val fetchingPages = mutableSetOf<Int>()
    private var conversations: ConversationsModel? = null
    private val fetchInboxRequestFlow = MutableSharedFlow<Int>()
    private var seen = mutableSetOf<Int>()

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

                clearData()

                inboxUpdate.setValue(
                    InboxUpdate(
                        inboxModel = InboxModel(),
                        scrollToTop = false,
                        isLoading = true,
                    ),
                )

                fetchInboxAsync(pageIndex = 0)
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
                    Log.d(TAG, "Page type changed")

                    clearData()

                    inboxUpdate.setValue(
                        InboxUpdate(
                            inboxModel = InboxModel(),
                            scrollToTop = false,
                            isLoading = true,
                        ),
                    )

                    fetchNextPage()
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
                    fetchInbox(it)
                }
        }
        inboxRepository.onServerChanged()
    }

    fun setPageType(pageType: PageType) {
        savedStateHandle["page_type"] = pageType
    }

    fun refresh(force: Boolean) {
        viewModelScope.launch {
            clearData()
            fetchInbox(0, force = force)
        }
    }

    fun fetchNextPage() {
        viewModelScope.launch {
            val currentPage = inboxUpdate.valueOrNull
                ?.inboxModel
                ?.items
                ?.maxOfOrNull { it.page }
                ?: -1
            val nextPage = currentPage + 1
            fetchInboxAsync(nextPage)
        }
    }

    fun fetchInboxAsync(pageIndex: Int) {
        viewModelScope.launch {
            fetchInboxRequestFlow.emit(pageIndex)
        }
    }

    fun fetchNewInboxItems() {
        val pageType = pageTypeFlow.value

        if (pageType == PageType.Messages) {
            // TODO: Fetch new conversations...
            return
        }

        viewModelScope.launch {
            val result = inboxRepository.getPage(
                pageIndex = 0,
                pageType = pageType,
                force = true,
                retainItemsOnForce = true
            )

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

                    publishInboxUpdate(scrollToTop = false)
                }
                .onFailure {
                    // silently fail
                }
        }

    }

    fun fetchInbox(pageIndex: Int, force: Boolean = false, retainItemsOnForce: Boolean = false) {
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
        Log.d(
            TAG,
            "Loading inbox page - " +
                "pageIndex: $pageIndex pageType: $pageType force: $force",
        )

        inboxUpdate.setIsLoading()
        fetchInboxJob = viewModelScope.launch {
            val result = inboxRepository.getPage(
                pageIndex = pageIndex,
                pageType = pageType,
                force = force,
                retainItemsOnForce = retainItemsOnForce
            )

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
                    markAsReadResult.postValue(Unit)
                }
                .onFailure {
                    markAsReadResult.postError(it)
                }
        }
    }

    fun markAllAsRead() {
        markAsReadResult.setIsLoading()

        allInboxItems.forEach {
            markAsReadInViewData(
                id = when (it) {
                    is InboxListItem.ConversationItem -> it.conversation.id
                    is InboxListItem.RegularInboxItem -> it.item.id.toLong()
                },
                isRead = true,
            )
        }

        viewModelScope.launch {
            apiClient.markAllAsRead()
                .onSuccess {
                    markAsReadResult.postValue(Unit)
                }
                .onFailure {
                    markAsReadResult.postError(it)
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
                                page = 0,
                            )
                        },
                        earliestMessageTs = conversations.conversationEarliestMessageTs,
                        hasMore = false,
                    ),
                    scrollToTop = scrollToTop,
                    isLoading = false,
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
                    isLoading = false,
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
                    allInboxItems[index] = data.copy(
                        conversation = data.conversation.copy(isRead = isRead))
                }
                is InboxListItem.RegularInboxItem -> {
                    if (data.item.id.toLong() != id) {
                        continue
                    }
                    allInboxItems[index] = data.copy(
                        item = data.item.updateIsRead(isRead = isRead) as InboxItem)
                }
            }
        }

        if (isLoaded) {
            publishInboxUpdate(scrollToTop = false)
        }
    }

    private fun addData(data: LemmyListSource.PageResult<InboxItem>) {
        hasMore = data.hasMore

        allInboxItems.addAll(data.items.mapNotNull {
            if (seen.contains(it.id)) {
                null
            } else {
                seen.add(it.id)
                InboxListItem.RegularInboxItem(data.pageIndex, it)
            }
        })
        allInboxItems.sortByDescending {
            when (it) {
                is InboxListItem.ConversationItem -> it.conversation.ts
                is InboxListItem.RegularInboxItem -> it.item.lastUpdateTs
            }
        }

        Log.d(TAG, "Data updated! Total items: ${allInboxItems.size} hasMore: $hasMore")
    }

    private fun clearData() {
        hasMore = true
        isLoaded = false
        allInboxItems.clear()
        fetchingPages.clear()
        seen.clear()
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
    val isLoading: Boolean,
)
