package com.idunnololz.summit.lemmy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.Post
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreActionsViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    val accountManager: AccountManager,
    val accountActionsManager: AccountActionsManager,
) : ViewModel() {
    val blockCommunityResult = StatefulLiveData<Unit>()
    val blockPersonResult = StatefulLiveData<BlockPersonResult>()
    val deletePostResult = StatefulLiveData<PostView>()

    fun blockCommunity(id: CommunityId) {
        blockCommunityResult.setIsLoading()
        viewModelScope.launch {
            apiClient.blockCommunity(id, true)
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
            apiClient.blockPerson(id, block)
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
            apiClient.deletePost(post.id)
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

    fun vote(postView: PostView, dir: Int) {
        accountActionsManager.vote(apiClient.instance, VotableRef.PostRef(postView.post.id), dir)
    }

    fun vote(commentView: CommentView, dir: Int) {
        accountActionsManager.vote(apiClient.instance, VotableRef.CommentRef(commentView.comment.id), dir)
    }
}

data class BlockPersonResult(
    val blockedPerson: Boolean
)