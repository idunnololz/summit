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
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreActionsViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    val accountManager: AccountManager,
    val actionsManager: AccountActionsManager,
) : ViewModel() {
    val blockCommunityResult = StatefulLiveData<Unit>()
    val blockPersonResult = StatefulLiveData<Unit>()
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

    fun blockPerson(id: PersonId) {
        blockPersonResult.setIsLoading()
        viewModelScope.launch {
            apiClient.blockPerson(id, true)
                .onFailure {
                    blockPersonResult.postError(it)
                }
                .onSuccess {
                    blockCommunityResult.postValue(Unit)
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

    fun upvote(postView: PostView) {
        actionsManager.vote(apiClient.instance, VotableRef.PostRef(postView.post.id), 1)
    }

    fun upvote(commentView: CommentView) {
        actionsManager.vote(apiClient.instance, VotableRef.CommentRef(commentView.comment.id), 1)
    }
}