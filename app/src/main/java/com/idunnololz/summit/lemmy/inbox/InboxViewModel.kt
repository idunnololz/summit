package com.idunnololz.summit.lemmy.inbox

import android.content.Context
import android.os.Parcelable
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
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import com.idunnololz.summit.notifications.NotificationsManager
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
    private val inboxRepositoryFactory: InboxRepository.Factory,
    private val notificationsManager: NotificationsManager,
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

    private val allData: MutableList<LemmyListSource.PageResult<InboxItem>> = mutableListOf()
    private var hasMore = true
    private val fetchingPages = mutableSetOf<Int>()

    val isUserOnInboxScreen = MutableStateFlow<Boolean>(false)
    val lastInboxUnreadLoadTimeMs = MutableStateFlow<Long>(0)
    var clearInboxNotificationsJob: Job? = null

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

                allData.clear()
                fetchingPages.clear()

                inboxUpdate.setValue(
                    InboxUpdate(
                        inboxData = allData,
                        scrollToTop = false,
                    ),
                )

                fetchInbox(pageIndex, requireNotNull(pageTypeFlow.value))
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

                    allData.clear()
                    fetchingPages.clear()

                    inboxUpdate.setValue(
                        InboxUpdate(
                            inboxData = allData,
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
        inboxRepository.onServerChanged()
    }

    fun fetchInbox(
        pageIndex: Int = this.pageIndex,
        pageType: PageType = requireNotNull(this.pageTypeFlow.value),
        force: Boolean = false,
    ) {
        if (fetchingPages.contains(pageIndex)) {
            return
        }

        fetchingPages.add(pageIndex)
        fetchInboxJob?.cancel()
        Log.d(TAG, "Loading inbox page $pageIndex. PageType: $pageType")

        inboxUpdate.setIsLoading()
        fetchInboxJob = viewModelScope.launch {
            val result = inboxRepository.getPage(pageIndex, pageType, force)

            ensureActive()

            result
                .onSuccess {
                    addData(it)

                    inboxUpdate.setValue(
                        InboxUpdate(
                            inboxData = allData,
                            scrollToTop = pageIndex == 0 && force,
                        ),
                    )

                    fetchingPages.remove(pageIndex)
                }
                .onFailure {
                    inboxUpdate.postError(it)

                    fetchingPages.remove(pageIndex)
                }
        }
    }

    fun markAsRead(
        inboxItem: InboxItem,
        read: Boolean,
        delete: Boolean = false,
        refreshAfter: Boolean = false,
    ) {
        markAsReadResult.setIsLoading()
        markAsReadInViewData(
            inboxItem.id,
            delete, // pageType.value == PageType.Unread,
            read,
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

    private fun markAsReadInViewData(id: Int, delete: Boolean = false, isRead: Boolean) {
        for ((index, data) in allData.withIndex()) {
            val position = data.items.indexOfFirst { it.id == id }
            if (position == -1) {
                Log.d(TAG, "Unable to find item to mark as read!")
                continue
            }

            val newItem = when (val item = data.items[position]) {
                is InboxItem.MentionInboxItem ->
                    item.copy(isRead = isRead)
                is InboxItem.MessageInboxItem ->
                    item.copy(isRead = isRead)
                is InboxItem.ReplyInboxItem ->
                    item.copy(isRead = isRead)
                is InboxItem.ReportMessageInboxItem ->
                    item.copy(isRead = isRead)
                is InboxItem.ReportCommentInboxItem ->
                    item.copy(isRead = isRead)
                is InboxItem.ReportPostInboxItem ->
                    item.copy(isRead = isRead)
            }

            allData[index] = data.copy(
                items = data.items.toMutableList().apply {
                    if (isRead && delete) {
                        removeAt(position)
                    } else {
                        this[position] = newItem
                    }
                },
            )
        }

        inboxUpdate.setValue(
            InboxUpdate(
                inboxData = allData,
                scrollToTop = false,
            ),
        )
    }

    private fun addData(data: LemmyListSource.PageResult<InboxItem>) {
        if (data.pageIndex == 0) {
            clearData()
        }

        val currentPageIndex = allData.lastOrNull()?.pageIndex ?: -1
        val nextPageIndex = currentPageIndex + 1

        if (data.pageIndex != nextPageIndex) {
            Log.d(
                TAG,
                "addData(): Data with unexpected page index. " +
                    "Expected $nextPageIndex. Got ${data.pageIndex}",
            )
            return
        }

        hasMore = data.hasMore

        allData.add(data)

        Log.d(TAG, "Data updated! Total items: ${allData.sumOf { it.items.size }}")
    }

    private fun clearData() {
        hasMore = true
        allData.clear()
        fetchingPages.clear()
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

    @Parcelize
    enum class PageType : Parcelable {
        Unread,
        All,
        Replies,
        Mentions,
        Messages,
        Reports,
    }
}

fun InboxViewModel.PageType.getName(context: Context) = when (this) {
    InboxViewModel.PageType.Unread -> context.getString(R.string.unread)
    InboxViewModel.PageType.All -> context.getString(R.string.all)
    InboxViewModel.PageType.Replies -> context.getString(R.string.replies)
    InboxViewModel.PageType.Mentions -> context.getString(R.string.mentions)
    InboxViewModel.PageType.Messages -> context.getString(R.string.messages)
    InboxViewModel.PageType.Reports -> context.getString(R.string.reports)
}

data class InboxUpdate(
    val inboxData: List<LemmyListSource.PageResult<InboxItem>>,
    val scrollToTop: Boolean,
)
