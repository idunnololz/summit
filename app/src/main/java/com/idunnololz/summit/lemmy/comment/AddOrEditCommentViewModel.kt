package com.idunnololz.summit.lemmy.comment

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.UploadImageResult
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.util.Event
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddOrEditCommentViewModel @Inject constructor(
    private val context: Application,
    private val apiClient: LemmyApiClient,
    private val accountManager: AccountManager,
    private val accountActionsManager: AccountActionsManager,
) : ViewModel() {
    val currentAccount = accountManager.currentAccount.asLiveData()

    val commentSentEvent = MutableLiveData<Event<Unit>>()
    val uploadImageEvent = StatefulLiveData<UploadImageResult>()

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

    fun uploadImage(instance: String, uri: Uri) {
        uploadImageEvent.setIsLoading()

        viewModelScope.launch {
            apiClient.changeInstance(instance)
            var result = uri.path
            val cut: Int? = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }

            val account = accountManager.currentAccount.value

            if (account == null) {
                uploadImageEvent.postError(NotAuthenticatedException())
                return@launch
            }
            context.contentResolver
                .openInputStream(uri)
                .use {
                    if (it == null) {
                        return@use Result.failure(RuntimeException("file_not_found"))
                    }
                    return@use apiClient.uploadImage(account, result ?: "image", it)
                }
                .onFailure {
                    uploadImageEvent.postError(it)
                }
                .onSuccess {
                    uploadImageEvent.postValue(it)
                }
        }
    }
}