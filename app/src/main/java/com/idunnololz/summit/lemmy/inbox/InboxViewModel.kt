package com.idunnololz.summit.lemmy.inbox

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.dto.CommentReplyView
import com.idunnololz.summit.api.dto.PersonMentionView
import com.idunnololz.summit.api.dto.PrivateMessageView
import com.idunnololz.summit.lemmy.person.PersonTabbedViewModel
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Qualifier

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
    private val inboxRepository: InboxRepository,
): ViewModel() {

    val currentAccount
        get() = accountManager.currentAccount.value
    val currentAccountView
        get() =
            currentAccount?.let {
                accountManager.getAccountViewForAccount(it)
            }
    val replies = StatefulLiveData<InboxResult.RepliesResult>()
    val mentions = StatefulLiveData<InboxResult.MentionsResult>()
    val messages = StatefulLiveData<InboxResult.MessagesResult>()

    var instance: String = apiClient.instance

    init {
        viewModelScope.launch {
            accountManager.currentAccountOnChange.collect {
                if (it != null) {
                    instance = it.instance
                }

                delay(10) // just in case it takes a second for the api client to update...
                fetchInbox(false)
            }
        }
    }

    fun fetchInboxIfNeeded() {
        fetchInbox(false)
    }

    fun fetchInbox(force: Boolean) {
        viewModelScope.launch {
            fetchReplies(force)
            fetchMentions(force)
            fetchMessages(force)
        }
    }

    fun fetchReplies(force: Boolean) {
        if (!force && replies.valueOrNull != null) return

        replies.setIsLoading()
        viewModelScope.launch {
            inboxRepository.repliesSource.getPage(0, force)
                .onSuccess {
                    replies.postValue(InboxResult.RepliesResult(it.items, it.hasMore))
                }
                .onFailure {
                    replies.postError(it)
                }
        }
    }

    fun fetchMentions(force: Boolean) {
        if (!force && mentions.valueOrNull != null) return

        mentions.setIsLoading()
        viewModelScope.launch {
            inboxRepository.mentionsSource.getPage(0, force)
                .onSuccess {
                    mentions.postValue(InboxResult.MentionsResult(it.items, it.hasMore))
                }
                .onFailure {
                    mentions.postError(it)
                }
        }
    }

    fun fetchMessages(force: Boolean) {
        if (!force && messages.valueOrNull != null) return

        messages.setIsLoading()
        viewModelScope.launch {
            inboxRepository.messagesSource.getPage(0, force)
                .onSuccess {
                    messages.postValue(InboxResult.MessagesResult(it.items, it.hasMore))
                }
                .onFailure {
                    messages.postError(it)
                }
        }
    }

    sealed interface InboxResult {

        val hasMore: Boolean

        data class RepliesResult(
            val replies: List<CommentReplyView>,
            override val hasMore: Boolean,
        ): InboxResult
        data class MentionsResult(
            val mentions: List<PersonMentionView>,
            override val hasMore: Boolean,
        ): InboxResult
        data class MessagesResult(
            val messages: List<PrivateMessageView>,
            override val hasMore: Boolean,
        ): InboxResult
    }



    @Parcelize
    enum class PageType : Parcelable {
        All,
        Replies,
        Mentions,
        Messages,
    }
}

fun InboxViewModel.PageType.getName(context: Context) =
    when (this) {
        InboxViewModel.PageType.All -> context.getString(R.string.all)
        InboxViewModel.PageType.Replies -> context.getString(R.string.replies)
        InboxViewModel.PageType.Mentions -> context.getString(R.string.mentions)
        InboxViewModel.PageType.Messages -> context.getString(R.string.messages)
    }