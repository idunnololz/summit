package com.idunnololz.summit.lemmy.comment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddOrEditCommentViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val accountActionsManager: AccountActionsManager,
) : ViewModel() {
    val currentAccount = accountManager.currentAccount.asLiveData()

    val commentSentEvent = MutableLiveData<Event<Unit>>()

    fun editComment() {
        accountActionsManager
    }

    fun sendComment(
        postRef: PostRef,
        parentId: CommentId?,
        content: String,
    ) {
        viewModelScope.launch {
            accountActionsManager.createComment(
                postRef,
                parentId,
                content,
            )

            commentSentEvent.postValue(Event(Unit))
        }
    }

    fun updateComment(postRef: PostRef, commentId: CommentId, content: String) {
        viewModelScope.launch {
            accountActionsManager.editComment(
                postRef,
                commentId,
                content,
            )

            commentSentEvent.postValue(Event(Unit))
        }
    }
}