package com.idunnololz.summit.lemmy.comment

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccountLiveData
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.AccountInstanceMismatchException
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftsManager
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.inbox.CommentBackedItem
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AddOrEditCommentViewModel @Inject constructor(
    private val context: Application,
    private val lemmyApiClientFactory: AccountAwareLemmyClient.Factory,
    val accountManager: AccountManager,
    private val accountActionsManager: AccountActionsManager,
    private val state: SavedStateHandle,
    private val preferences: Preferences,
    val draftsManager: DraftsManager,
) : ViewModel() {

    companion object {
        private const val TAG = "AddOrEditCommentViewModel"
    }

    sealed interface Message {
        data class ReplyTargetTooOld(
            val replyTargetTs: Long,
        ) : Message
    }

    /**
     * Create a new instance so we can change the instance without screwing up app state
     */
    val lemmyApiClient = lemmyApiClientFactory.create()

    val currentAccount = accountManager.currentAccount.asAccountLiveData()

    val commentSentEvent = StatefulLiveData<Unit>()

    val currentDraftEntry = state.getLiveData<DraftEntry>("current_draft_entry")

    val messages = MutableLiveData<List<Message>>(listOf())

    fun sendComment(account: Account, postRef: PostRef, parentId: CommentId?, content: String) {
        viewModelScope.launch {
            if (postRef.instance != account.instance) {
                commentSentEvent.postError(
                    AccountInstanceMismatchException(
                        account.instance,
                        postRef.instance,
                    ),
                )
                return@launch
            }

            accountActionsManager.createComment(
                postRef,
                parentId,
                content,
                account.id,
            )

            commentSentEvent.postValue(Unit)
        }
    }

    fun sendComment(account: Account, instance: String, inboxItem: InboxItem, content: String) {
        viewModelScope.launch {
            if (inboxItem is CommentBackedItem) {
                if (instance != account.instance) {
                    commentSentEvent.postError(
                        AccountInstanceMismatchException(
                            account.instance,
                            instance,
                        ),
                    )
                    return@launch
                }

                accountActionsManager.createComment(
                    PostRef(instance, inboxItem.postId),
                    inboxItem.commentId,
                    content,
                    account.id,
                )

                commentSentEvent.postValue(Unit)
            } else {
                when (inboxItem) {
                    is InboxItem.MentionInboxItem,
                    is InboxItem.ReplyInboxItem,
                    is InboxItem.ReportMessageInboxItem,
                    is InboxItem.ReportCommentInboxItem,
                    is InboxItem.ReportPostInboxItem,
                    -> error("Should never happen!")
                    is InboxItem.MessageInboxItem -> {
                        lemmyApiClient
                            .createPrivateMessage(
                                content = content,
                                recipient = inboxItem.authorId,
                                account = requireNotNull(currentAccount.value),
                            )
                            .onFailure {
                                commentSentEvent.postError(it)
                            }
                            .onSuccess {
                                commentSentEvent.postValue(Unit)
                            }
                    }
                }
            }
        }
    }

    fun updateComment(account: Account, postRef: PostRef, commentId: CommentId, content: String) {
        viewModelScope.launch {
            accountActionsManager.editComment(
                postRef,
                commentId,
                content,
                account.id,
            )

            commentSentEvent.postValue(Unit)
        }
    }

    fun sendComment(account: Account, personRef: PersonRef, content: String) {
        viewModelScope.launch {
            lemmyApiClient.setAccount(account, accountChanged = true)
            lemmyApiClient
                .fetchPersonByNameWithRetry(
                    name = personRef.fullName,
                    sortType = SortType.New,
                    page = 1,
                    limit = 1,
                    force = false,
                )
                .onSuccess {
                    sendComment(
                        account,
                        it.person_view.person.id,
                        content,
                    )
                }
                .onFailure {
                    commentSentEvent.postError(it)
                }
        }
    }

    fun sendComment(account: Account, personId: PersonId, content: String) {
        viewModelScope.launch {
            lemmyApiClient.setAccount(account, accountChanged = true)
            lemmyApiClient
                .createPrivateMessage(
                    content = content,
                    recipient = personId,
                    account = requireNotNull(currentAccount.value),
                )
                .onFailure {
                    commentSentEvent.postError(it)
                }
                .onSuccess {
                    commentSentEvent.postValue(Unit)
                }
        }
    }

    fun addMessage(message: Message) {
        messages.value = (messages.value ?: listOf()) + message
    }

    fun dismissMessage(message: Message) {
        messages.value = (messages.value ?: listOf()).filter { it != message }
    }
}
