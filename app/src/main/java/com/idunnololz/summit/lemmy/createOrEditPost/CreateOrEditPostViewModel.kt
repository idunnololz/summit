package com.idunnololz.summit.lemmy.createOrEditPost

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.right
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.UploadImageResult
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateOrEditPostViewModel @Inject constructor(
    private val context: Application,
    private val apiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
) : ViewModel() {

    var postPrefilled: Boolean = false
    val createOrEditPostResult = StatefulLiveData<PostView>()
    val uploadImageResult = StatefulLiveData<UploadImageResult>()
    val uploadImageForUrlResult = StatefulLiveData<UploadImageResult>()

    fun createPost(
        communityRef: CommunityRef.CommunityRefByName,
        name: String,
        body: String,
        url: String,
        isNsfw: Boolean,
    ) {
        createOrEditPostResult.setIsLoading()
        viewModelScope.launch {
            val communityIdResult =
                apiClient.fetchCommunityWithRetry(
                    Either.Right(communityRef.fullName), force = false
                )
                    .fold(
                        {
                            Result.success(it.community.id)
                        },
                        {
                            Result.failure(it)
                        }
                    )

            if (communityIdResult.isFailure) {
                createOrEditPostResult.postError(requireNotNull(communityIdResult.exceptionOrNull()))
                return@launch
            }

            if (name.isBlank()) {
                createOrEditPostResult.postError(NoTitleError())
                return@launch
            }

            val result = apiClient.createPost(
                name = name,
                body = body.ifBlank { null },
                url = url.ifBlank { null },
                isNsfw = isNsfw,
                communityId = communityIdResult.getOrThrow()
            )

            result
                .onSuccess {
                    createOrEditPostResult.postValue(it)
                }
                .onFailure {
                    createOrEditPostResult.postError(it)
                }
        }
    }

    fun updatePost(
        instance: String,
        name: String,
        body: String,
        url: String,
        isNsfw: Boolean,
        postId: PostId,
    ) {
        createOrEditPostResult.setIsLoading()
        viewModelScope.launch {
            val account = accountManager.currentAccount.value

            if (account == null) {
                createOrEditPostResult.postError(NotAuthenticatedException())
                return@launch
            }

            if (name.isBlank()) {
                createOrEditPostResult.postError(NoTitleError())
                return@launch
            }

            val result = apiClient.editPost(
                postId = postId,
                name = name,
                body = body.ifBlank { null },
                url = url.ifBlank { null },
                isNsfw = isNsfw,
                account = account,
            )

            result
                .onSuccess {
                    createOrEditPostResult.postValue(it)
                }
                .onFailure {
                    createOrEditPostResult.postError(it)
                }
        }
    }

    fun uploadImage(instance: String, uri: Uri) {
        uploadImageInternal(instance, uri, uploadImageResult)
    }

    fun uploadImageForUrl(instance: String, uri: Uri) {
        uploadImageInternal(instance, uri, uploadImageForUrlResult)
    }

    private fun uploadImageInternal(
        instance: String,
        uri: Uri,
        imageLiveData: StatefulLiveData<UploadImageResult>
    ) {
        imageLiveData.setIsLoading()

        viewModelScope.launch {
            var result = uri.path
            val cut: Int? = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }

            val account = accountManager.currentAccount.value

            if (account == null) {
                imageLiveData.postError(NotAuthenticatedException())
                return@launch
            }
            context.contentResolver
                .openInputStream(uri)
                .use {
                    if (it == null) {
                        return@use Result.failure(RuntimeException("file_not_found"))
                    }
                    return@use apiClient.uploadImage(result ?: "image", it)
                }
                .onFailure {
                    imageLiveData.postError(it)
                }
                .onSuccess {
                    imageLiveData.postValue(it)
                }
        }
    }

    class NoTitleError : RuntimeException()
}