package com.idunnololz.summit.lemmy.inbox

import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
    private val inboxRepositoryFactory: InboxRepository.Factory,
) : ViewModel() {

    companion object {
        private const val TAG = "InboxViewModel"
    }

    private var fetchInboxJob: Job? = null

    var inboxRepository = inboxRepositoryFactory.create()

    val currentAccount
        get() = accountManager.currentAccount.asLiveData()
    val currentAccountView = MutableLiveData<AccountView?>()
    val markAsReadResult = StatefulLiveData<Unit>()

    val inboxData = StatefulLiveData<List<LemmyListSource.PageResult<InboxItem>>>()

    val pageTypeFlow = MutableStateFlow<PageType>(PageType.Unread)

    val pageType = pageTypeFlow.asLiveData()

    var pageIndex = 0

    var instance: String = apiClient.instance

    var pauseUnreadUpdates = false

    private val allData: MutableList<LemmyListSource.PageResult<InboxItem>> = mutableListOf()
    private var hasMore = true

    init {
        viewModelScope.launch {
            accountManager.currentAccountOnChange.collect {
                if (it != null) {
                    instance = it.instance
                }

                inboxRepository = inboxRepositoryFactory.create()

                delay(10) // just in case it takes a second for the api client to update...
                pageIndex = 0

                allData.clear()
                inboxData.setValue(allData)

                fetchInbox(pageIndex, requireNotNull(pageTypeFlow.value))
            }
        }

        viewModelScope.launch {
            accountManager.currentAccount.collect {
                withContext(Dispatchers.Main) {
                    if (it != null) {
                        currentAccountView.value = accountInfoManager.getAccountViewForAccount(it)
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
                    inboxData.setValue(allData)

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
        inboxRepository.onServerChanged()
    }

    fun fetchInbox(
        pageIndex: Int = this.pageIndex,
        pageType: PageType = requireNotNull(this.pageTypeFlow.value),
        force: Boolean = false,
    ) {
        fetchInboxJob?.cancel()
        Log.d(TAG, "Loading inbox page $pageIndex. PageType: $pageType")

        inboxData.setIsLoading()
        fetchInboxJob = viewModelScope.launch {
            val result = inboxRepository.getPage(pageIndex, pageType, force)

            ensureActive()

            result
                .onSuccess {
                    addData(it)
                    inboxData.postValue(allData)
                }
                .onFailure {
                    inboxData.postError(it)
                }
        }
    }

    fun markAsRead(inboxItem: InboxItem, read: Boolean, delete: Boolean = false) {
        markAsReadResult.setIsLoading()
        markAsReadInViewData(
            inboxItem.id,
            delete, // pageType.value == PageType.Unread,
            read,
        )
        viewModelScope.launch {
            inboxRepository.markAsRead(inboxItem, read)
                .onSuccess {
                    if (!read) {
                        fetchInbox()
                    }
                    markAsReadResult.postValue(Unit)
                }
                .onFailure {
                    fetchInbox()
                    markAsReadResult.postError(it)
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

        inboxData.setValue(allData)
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
    }

    override fun onCleared() {
        fetchInboxJob?.cancel()
        super.onCleared()
    }

    @Parcelize
    enum class PageType : Parcelable {
        Unread,
        All,
        Replies,
        Mentions,
        Messages,
    }
}

fun InboxViewModel.PageType.getName(context: Context) =
    when (this) {
        InboxViewModel.PageType.Unread -> context.getString(R.string.unread)
        InboxViewModel.PageType.All -> context.getString(R.string.all)
        InboxViewModel.PageType.Replies -> context.getString(R.string.replies)
        InboxViewModel.PageType.Mentions -> context.getString(R.string.mentions)
        InboxViewModel.PageType.Messages -> context.getString(R.string.messages)
    }
