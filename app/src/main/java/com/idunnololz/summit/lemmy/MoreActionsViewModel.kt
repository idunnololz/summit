package com.idunnololz.summit.lemmy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.actions.SavedManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.AccountInstanceMismatchException
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.Post
import com.idunnololz.summit.api.dto.PostFeatureType
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
    val accountInfoManager: AccountInfoManager,
    val accountActionsManager: AccountActionsManager,
    private val hiddenPostsManager: HiddenPostsManager,
    private val savedManager: SavedManager,
) : ViewModel() {

    enum class PostActionType {
        DeletePost,
        SavePost,
        FeaturePost,
        LockPost,
        RemovePost,
    }

    data class PostAction(
        val actionType: PostActionType,
        val state: StatefulLiveData<PostView> = StatefulLiveData(),
    )

    val currentAccount: Account?
        get() = apiClient.accountForInstance()
    val apiInstance: String
        get() = apiClient.instance

    val blockCommunityResult = StatefulLiveData<Unit>()
    val blockPersonResult = StatefulLiveData<BlockPersonResult>()
    val saveCommentResult = StatefulLiveData<CommentView>()
    val banUserResult = StatefulLiveData<Unit>()
    val modUserResult = StatefulLiveData<Unit>()
    val distinguishCommentResult = StatefulLiveData<Unit>()
    val removeCommentResult = StatefulLiveData<Unit>()

    val deletePostAction = PostAction(actionType = PostActionType.DeletePost)
    val savePostAction = PostAction(actionType = PostActionType.SavePost)
    val featurePostAction = PostAction(actionType = PostActionType.FeaturePost)
    val lockPostAction = PostAction(actionType = PostActionType.LockPost)
    val removePostAction = PostAction(actionType = PostActionType.RemovePost)

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

    fun deletePost(postId: PostId) {
        deletePostAction.state.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance { apiClient.deletePost(postId) }
                .onSuccess {
                    deletePostAction.state.postValue(it)
                }
                .onFailure {
                    deletePostAction.state.postError(it)
                }
        }
    }

    fun featurePost(postId: PostId, feature: Boolean) {
        featurePostAction.state.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.featurePost(postId, feature, PostFeatureType.Community)
                    .onSuccess {
                        featurePostAction.state.postValue(it)
                    }
                    .onFailure {
                        featurePostAction.state.postError(it)
                    }
            }
        }
    }

    fun removePost(postId: PostId, remove: Boolean) {
        removePostAction.state.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.removePost(postId, remove, null)
                    .onSuccess {
                        removePostAction.state.postValue(it)
                    }
                    .onFailure {
                        removePostAction.state.postError(it)
                    }
            }
        }
    }

    fun lockPost(postId: PostId, lock: Boolean) {
        lockPostAction.state.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.lockPost(postId, lock)
                    .onSuccess {
                        lockPostAction.state.postValue(it)
                    }
                    .onFailure {
                        lockPostAction.state.postError(it)
                    }
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
        savePostAction.state.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance { apiClient.savePost(id, save) }
                .onSuccess {
                    savePostAction.state.postValue(it)
                    savedManager.onPostSaveChanged()
                }
                .onFailure {
                    savePostAction.state.postError(it)
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

    fun banUser(
        communityId: CommunityId,
        personId: PersonId,
        ban: Boolean,
        removeData: Boolean,
        reason: String?,
        expiresDays: Int?,
    ) {
        banUserResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.banUserFromCommunity(
                    communityId,
                    personId,
                    ban,
                    removeData,
                    reason,
                    expiresDays,
                )
            }
                .onSuccess {
                    banUserResult.postValue(Unit)
                }
                .onFailure {
                    banUserResult.postError(it)
                }
        }
    }

    fun mod(communityId: Int, personId: Int, mod: Boolean) {
        modUserResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.modUser(
                    communityId,
                    personId,
                    mod,
                )
            }
                .onSuccess {
                    modUserResult.postValue(Unit)
                }
                .onFailure {
                    modUserResult.postError(it)
                }
        }
    }

    fun distinguishComment(
        commentId: CommentId,
        distinguish: Boolean,
    ) {
        distinguishCommentResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.distinguishComment(
                    commentId,
                    distinguish,
                )
            }
                .onSuccess {
                    distinguishCommentResult.postValue(Unit)
                }
                .onFailure {
                    distinguishCommentResult.postError(it)
                }
        }
    }

    fun removeComment(commentId: Int, remove: Boolean) {
        removeCommentResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.removeComment(
                    commentId,
                    remove,
                    null,
                )
            }
                .onSuccess {
                    removeCommentResult.postValue(Unit)
                }
                .onFailure {
                    removeCommentResult.postError(it)
                }
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
