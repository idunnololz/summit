package com.idunnololz.summit.lemmy.comment

import android.app.Application
import android.net.Uri
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
import com.idunnololz.summit.api.UploadImageResult
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftsManager
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.UploadHelper
import com.idunnololz.summit.lemmy.inbox.CommentBackedItem
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.Event
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddOrEditCommentViewModel @Inject constructor(
    private val context: Application,
    private val authedApiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
    private val accountActionsManager: AccountActionsManager,
    private val state: SavedStateHandle,
    private val preferences: Preferences,
    private val uploadHelper: UploadHelper,
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

    val currentAccount = accountManager.currentAccount.asAccountLiveData()

    val commentSentEvent = MutableLiveData<Event<Result<Unit>>>()
    val uploadImageEvent = StatefulLiveData<UploadImageResult>()

    val currentDraftEntry = state.getLiveData<DraftEntry>("current_draft_entry")

    val messages = MutableLiveData<List<Message>>(listOf())

    fun editComment() {
        accountActionsManager
    }

    fun sendComment(
        account: Account,
        postRef: PostRef,
        parentId: CommentId?,
        content: String,
    ) {
        viewModelScope.launch {
            if (postRef.instance != account.instance) {
                commentSentEvent.postValue(
                    Event(
                        Result.failure(
                            AccountInstanceMismatchException(
                                account.instance,
                                postRef.instance,
                            ),
                        ),
                    ),
                )
                return@launch
            }

            accountActionsManager.createComment(
                postRef,
                parentId,
                content,
            )

            commentSentEvent.postValue(Event(Result.success(Unit)))
        }
    }

    fun sendComment(
        account: Account,
        instance: String,
        inboxItem: InboxItem,
        content: String,
    ) {
        viewModelScope.launch {
            if (inboxItem is CommentBackedItem) {
                if (instance != account.instance) {
                    commentSentEvent.postValue(
                        Event(
                            Result.failure(
                                AccountInstanceMismatchException(
                                    account.instance,
                                    instance,
                                ),
                            ),
                        ),
                    )
                    return@launch
                }

                accountActionsManager.createComment(
                    PostRef(instance, inboxItem.postId),
                    inboxItem.commentId,
                    content,
                )

                commentSentEvent.postValue(Event(Result.success(Unit)))
            } else {
                when (inboxItem) {
                    is InboxItem.MentionInboxItem,
                    is InboxItem.ReplyInboxItem,
                    is InboxItem.ReportMessageInboxItem,
                    is InboxItem.ReportCommentInboxItem,
                    is InboxItem.ReportPostInboxItem,
                    -> error("Should never happen!")
                    is InboxItem.MessageInboxItem -> {
                        authedApiClient
                            .createPrivateMessage(
                                content = content,
                                recipient = inboxItem.authorId,
                                account = requireNotNull(currentAccount.value),
                            )
                            .onFailure {
                                commentSentEvent.postValue(Event(Result.failure(it)))
                            }
                            .onSuccess {
                                commentSentEvent.postValue(Event(Result.success(Unit)))
                            }
                    }
                }
            }
        }
    }

    fun updateComment(postRef: PostRef, commentId: CommentId, content: String) {
        viewModelScope.launch {
            accountActionsManager.editComment(
                postRef,
                commentId,
                content,
            )

            commentSentEvent.postValue(Event(Result.success(Unit)))
        }
    }

    fun uploadImage(instance: String, uri: Uri) {
        uploadImageEvent.setIsLoading()

        uploadHelper.upload(
            coroutineScope = viewModelScope,
            uri = uri,
            rotateAccounts = preferences.rotateInstanceOnUploadFail,
            onSuccess = {
                uploadImageEvent.postValue(it)
            },
            onFailure = {
                uploadImageEvent.postError(it)
            },
        )
    }

    fun sendComment(personId: PersonId, content: String) {
        viewModelScope.launch {
            authedApiClient
                .createPrivateMessage(
                    content = content,
                    recipient = personId,
                    account = requireNotNull(currentAccount.value),
                )
                .onFailure {
                    commentSentEvent.postValue(Event(Result.failure(it)))
                }
                .onSuccess {
                    commentSentEvent.postValue(Event(Result.success(Unit)))
                }
        }
    }

    fun addMessage(message: Message) {
        messages.value = (messages.value ?: listOf()) + message
    }

    fun dismissMessage(message: Message) {
        messages.value = (messages.value ?: listOf()).filter { it != message }
    }

    val isUploading: Boolean
        get() = uploadHelper.isUploading
}
