package com.idunnololz.summit.feedback

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccountLiveData
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftsManager
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.UploadHelper
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.changeLogPostRef as changeLogPostRef1
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class PostFeedbackViewModel @Inject constructor(
    private val state: SavedStateHandle,
    private val lemmyClient: AccountAwareLemmyClient,
    private val unauthedApiClient: LemmyApiClient,
    private val accountActionsManager: AccountActionsManager,
    private val uploadHelper: UploadHelper,
    val accountManager: AccountManager,
    val draftsManager: DraftsManager,
) : ViewModel() {

    companion object {
        private const val TAG = "PostFeedbackViewModel"
    }

    val currentDraftEntry = state.getLiveData<DraftEntry>("current_draft_entry")
    val currentDraftId = state.getLiveData<Long>("current_draft_id")

    val apiInstance
        get() = lemmyClient.instance
    val currentAccount = accountManager.currentAccount.asAccountLiveData()
    val changeLogPostRef = changeLogPostRef1

    val postFeedbackState = StatefulLiveData<Unit>()

    fun postFeedbackToChangelogPost(text: String, screenshot: File?) {
        unauthedApiClient.changeInstance(changeLogPostRef.instance)

        val account = currentAccount.value
        var finalText = text

        if (account == null) {
            postFeedbackState.setError(NotAuthenticatedException())
            return
        }

        val newInstance = account.instance

        postFeedbackState.setIsLoading()

        viewModelScope.launch(Dispatchers.Default) {
            val linkToResolve =
                unauthedApiClient.fetchPost(null, Either.Left(changeLogPostRef.id), force = false)
                    .fold(
                        onSuccess = {
                            Result.success(it.post.ap_id)
                        },
                        onFailure = {
                            Result.failure(it)
                        },
                    )

            linkToResolve.onFailure {
                postFeedbackState.postError(it)
                return@launch
            }

            val postResponse = lemmyClient.resolveObject(linkToResolve.getOrThrow())

            postResponse.onFailure {
                Log.e(TAG, "Error resolving object.", it)
                postFeedbackState.postError(it)
                return@launch
            }

            val post = postResponse.getOrThrow().post

            if (post == null) {
                postFeedbackState.postError(RuntimeException())
                return@launch
            }

            if (screenshot != null) {
                uploadHelper.uploadFile(screenshot, false)
                val uploadResult = screenshot.inputStream().use { inputStream ->
                    lemmyClient.uploadImage("image", inputStream)
                }

                uploadResult.onFailure {
                    postFeedbackState.postError(it)
                    return@launch
                }

                finalText += "\n\n![](${uploadResult.getOrThrow().url})"
            }

            val postRef = PostRef(newInstance, post.post.id)
            accountActionsManager.createComment(
                postRef = postRef,
                parentId = null,
                content = finalText,
                accountId = account.id,
            )
            postFeedbackState.postValue(Unit)
        }
    }
}
