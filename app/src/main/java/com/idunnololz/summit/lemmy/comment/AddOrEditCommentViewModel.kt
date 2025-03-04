package com.idunnololz.summit.lemmy.comment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccountLiveData
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.AccountInstanceMismatchException
import com.idunnololz.summit.api.CommentsFetcher
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftsManager
import com.idunnololz.summit.filterLists.ContentFiltersManager
import com.idunnololz.summit.lemmy.CommentNodeData
import com.idunnololz.summit.lemmy.CommentTreeBuilder
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.inbox.CommentBackedItem
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@HiltViewModel
class AddOrEditCommentViewModel @Inject constructor(
    private val lemmyApiClientFactory: AccountAwareLemmyClient.Factory,
    val accountManager: AccountManager,
    private val accountActionsManager: AccountActionsManager,
    private val state: SavedStateHandle,
    private val contentFiltersManager: ContentFiltersManager,
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
    private val lemmyApiClient = lemmyApiClientFactory.create()

    private val commentsFetcher = CommentsFetcher(lemmyApiClient)

    val apiInstance: String
        get() = lemmyApiClient.instance

    val currentAccount = accountManager.currentAccount.asAccountLiveData()

    val commentSentEvent = StatefulLiveData<Unit>()

    val currentDraftEntry = state.getLiveData<DraftEntry>("current_draft_entry")
    val currentDraftId = state.getLiveData<Long>("current_draft_id")

    val messages = MutableLiveData<List<Message>>(listOf())

    val contextModel = StatefulLiveData<ContextModel>()

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

    fun showFullContext(postOrComment: Either<PostView, CommentView>, force: Boolean) {
        contextModel.setIsLoading()

        val postId = postOrComment.fold(
            { it.post.id },
            { it.post.id },
        )
        val commentPath = postOrComment.fold(
            { null },
            { it.comment.path },
        )

        viewModelScope.launch {
            val finalTopCommentId = if (commentPath != null) {
                val commentIds = commentPath.split(".").map { it.toInt() }
                val topCommentId = commentIds.firstOrNull { it != 0 }

                if (topCommentId == null) {
                    contextModel.setError(RuntimeException("No context found."))
                    return@launch
                }
                topCommentId
            } else {
                null
            }

            val postJob = async {
                lemmyApiClient.fetchPostWithRetry(Either.Left(postId), force)
            }
            val commentJob =
                if (finalTopCommentId != null) {
                    async {
                        commentsFetcher
                            .fetchCommentsWithRetry(
                                Either.Right(finalTopCommentId),
                                CommentSortType.Top,
                                null,
                                force,
                            )
                    }
                } else {
                    null
                }

            val postResult = postJob.await()
            val commentResult = commentJob?.await()

            if (postResult.isFailure) {
                contextModel.setError(requireNotNull(postResult.exceptionOrNull()))
                return@launch
            }

            if (commentResult?.isFailure == true) {
                contextModel.setError(requireNotNull(commentResult.exceptionOrNull()))
                return@launch
            }

            val tree = CommentTreeBuilder(
                accountManager,
                contentFiltersManager,
            ).buildCommentsTreeListView(
                post = null,
                comments = commentResult?.getOrNull(),
                parentComment = true,
                pendingComments = null,
                supplementaryComments = mapOf(),
                removedCommentIds = setOf(),
                fullyLoadedCommentIds = setOf(),
                targetCommentRef = null,
            )

            contextModel.postValue(
                ContextModel(
                    originalCommentView = postOrComment.getOrNull(),
                    post = requireNotNull(postResult.getOrNull()),
                    commentTree = tree.firstOrNull(),
                ),
            )
        }
    }

    data class ContextModel(
        val originalCommentView: CommentView?,
        val post: PostView,
        val commentTree: CommentNodeData?,
    )
}
