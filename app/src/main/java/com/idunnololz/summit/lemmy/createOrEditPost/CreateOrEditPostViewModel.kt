package com.idunnololz.summit.lemmy.createOrEditPost

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.UploadImageResult
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateOrEditPostViewModel @Inject constructor(
    private val context: Application,
    private val apiClient: LemmyApiClient,
    private val accountManager: AccountManager,
) : ViewModel() {

    var postPrefilled: Boolean = false
    val createOrEditPostResult = StatefulLiveData<PostView>()
    val uploadImageResult = StatefulLiveData<UploadImageResult>()
    val uploadImageForUrlResult = StatefulLiveData<UploadImageResult>()

    fun createPost(
        instance: String,
        name: String,
        body: String,
        url: String,
        isNsfw: Boolean,
        communityNameOrId: Either<String, CommunityId>,
    ) {
        createOrEditPostResult.setIsLoading()
        viewModelScope.launch {
            apiClient.changeInstance(instance)

            val account = accountManager.currentAccount.value
            val communityIdResult = communityNameOrId.fold(
                {
                    apiClient.getCommunity(account, Either.Right(it), false)
                        .fold(
                            {
                                Result.success(it.community.id)
                            },
                            {
                                Result.failure(it)
                            }
                        )
                },
                {
                    Result.success(it)
                }
            )

            if (communityIdResult.isFailure) {
                createOrEditPostResult.postError(requireNotNull(communityIdResult.exceptionOrNull()))
                return@launch
            }

            if (account == null) {
                createOrEditPostResult.postError(NotAuthenticatedException())
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
                account = account,
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
            apiClient.changeInstance(instance)

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
            apiClient.changeInstance(instance)
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
                    return@use apiClient.uploadImage(account, result ?: "image", it)
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