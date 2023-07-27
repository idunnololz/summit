package com.idunnololz.summit.lemmy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.SavedManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.AccountInstanceMismatchException
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.Post
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.hidePosts.HiddenPostsManager
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreActionsViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    val accountManager: AccountManager,
    val accountActionsManager: AccountActionsManager,
    private val hiddenPostsManager: HiddenPostsManager,
    private val savedManager: SavedManager,
) : ViewModel() {

    val apiInstance: String
        get() = apiClient.instance

    val blockCommunityResult = StatefulLiveData<Unit>()
    val blockPersonResult = StatefulLiveData<BlockPersonResult>()
    val deletePostResult = StatefulLiveData<PostView>()
    val savePostResult = StatefulLiveData<PostView>()
    val saveCommentResult = StatefulLiveData<CommentView>()

    private var currentPageInstance: String? = null

    fun blockCommunity(id: CommunityId, block: Boolean = true) {
        blockCommunityResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance { apiClient.blockCommunity(id, block) }
                .onFailure {
                    blockCommunityResult.postError(it)
                }
                .onSuccess {
                    blockCommunityResult.postValue(Unit)
                }
        }
    }

    fun blockPerson(id: PersonId, block: Boolean = true) {
        blockPersonResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance { apiClient.blockPerson(id, block) }
                .onFailure {
                    blockPersonResult.postError(it)
                }
                .onSuccess {
                    blockPersonResult.postValue(BlockPersonResult(block))
                }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            ensureRightInstance { apiClient.deletePost(post.id) }
                .onSuccess {
                    deletePostResult.postValue(it)
                }
                .onFailure {
                    deletePostResult.postError(it)
                }
        }
    }

    fun deleteComment(postRef: PostRef, commentId: Int) {
        viewModelScope.launch {
            accountActionsManager.deleteComment(postRef, commentId)
        }
    }

    fun vote(postView: PostView, dir: Int, toggle: Boolean = false) {
        val ref = VotableRef.PostRef(postView.post.id)
        val finalDir = if (toggle) {
            val curScore = accountActionsManager.getVote(ref)
                ?: postView.my_vote
            if (curScore == dir) {
                0
            } else {
                dir
            }
        } else {
            dir
        }
        accountActionsManager.vote(apiClient.instance, ref, finalDir)
    }

    fun vote(commentView: CommentView, dir: Int, toggle: Boolean = false) {
        val ref = VotableRef.CommentRef(commentView.comment.id)
        val finalDir = if (toggle) {
            val curScore = accountActionsManager.getVote(ref)
                ?: commentView.my_vote
            if (curScore == dir) {
                0
            } else {
                dir
            }
        } else {
            dir
        }
        accountActionsManager.vote(apiClient.instance, ref, finalDir)
    }

    fun savePost(id: PostId, save: Boolean) {
        viewModelScope.launch {
            ensureRightInstance { apiClient.savePost(id, save) }
                .onSuccess {
                    savePostResult.postValue(it)
                    savedManager.onPostSaveChanged()
                }
                .onFailure {
                    savePostResult.postError(it)
                }
        }
    }

    fun saveComment(id: CommentId, save: Boolean) {
        viewModelScope.launch {
            ensureRightInstance { apiClient.saveComment(id, save) }
                .onSuccess {
                    saveCommentResult.postValue(it)
                    savedManager.onCommentSaveChanged()
                }
                .onFailure {
                    saveCommentResult.postError(it)
                }
        }
    }

    fun hidePost(id: PostId) {
        hiddenPostsManager.hidePost(id, apiClient.instance)
    }

    fun onPostRead(postView: PostView, delayMs: Long) {
        if (postView.read) {
            return
        }

        viewModelScope.launch {
            if (delayMs > 0) {
                delay(delayMs)
            }
            accountActionsManager.markPostAsRead(apiClient.instance, postView.post.id, read = true)
        }
    }

    fun setPageInstance(instance: String) {
        currentPageInstance = instance
    }

    private suspend fun <T> ensureRightInstance(
        onCorrectInstance: suspend () -> Result<T>,
    ): Result<T> {
        val currentPageInstance = currentPageInstance
        return if (currentPageInstance != null && currentPageInstance != apiInstance) {
            Result.failure(
                AccountInstanceMismatchException(
                    apiInstance,
                    currentPageInstance,
                ),
            )
        } else {
            onCorrectInstance()
        }
    }
}

data class BlockPersonResult(
    val blockedPerson: Boolean,
)
