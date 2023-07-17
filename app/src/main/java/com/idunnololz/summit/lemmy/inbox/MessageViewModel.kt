package com.idunnololz.summit.lemmy.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.CommentsFetcher
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.Post
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.CommentNodeData
import com.idunnololz.summit.lemmy.CommentTreeBuilder
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountActionsManager: AccountActionsManager,
    val accountManager: AccountManager,
) : ViewModel() {

    private val commentsFetcher = CommentsFetcher(apiClient, accountActionsManager)

    val commentContext = StatefulLiveData<CommentContext>()
    var isContextShowing = false

    fun fetchCommentContext(postId: Int, commentPath: String, force: Boolean) {
        commentContext.setIsLoading()

        viewModelScope.launch {
            val commentIds = commentPath.split(".").map { it.toInt() }
            val topCommentId = commentIds.firstOrNull { it != 0 }

            if (topCommentId == null) {
                commentContext.setError(RuntimeException("No context found."))
                return@launch
            }

            val postJob = async {
                apiClient.fetchPostWithRetry(Either.Left(postId), force)
            }
            val commentJob = async {
                commentsFetcher
                    .fetchCommentsWithRetry(
                        Either.Right(topCommentId),
                        CommentSortType.Top,
                        force
                    )
            }

            val postResult = postJob.await()
            val commentResult = commentJob.await()

            if (postResult.isFailure) {
                commentContext.setError(requireNotNull(postResult.exceptionOrNull()))
                return@launch
            }

            if (commentResult.isFailure) {
                commentContext.setError(requireNotNull(commentResult.exceptionOrNull()))
                return@launch
            }

            val tree = CommentTreeBuilder(accountManager).buildCommentsTreeListView(
                post = null,
                comments = commentResult.getOrNull(),
                parentComment = true,
                pendingComments = null,
                supplementaryComments = mapOf()
            )

            commentContext.postValue(
                CommentContext(
                    requireNotNull(postResult.getOrNull()),
                    tree.first(),
                )
            )
        }
    }

    data class CommentContext(
        val post: PostView,
        val commentTree: CommentNodeData,
    )
}