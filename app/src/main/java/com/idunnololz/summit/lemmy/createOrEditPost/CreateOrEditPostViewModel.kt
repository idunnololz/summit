package com.idunnololz.summit.lemmy.createOrEditPost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.ApiException
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.lang.RuntimeException
import javax.inject.Inject

@HiltViewModel
class CreateOrEditPostViewModel @Inject constructor(
    private val apiClient: LemmyApiClient,
    private val accountManager: AccountManager,
) : ViewModel() {

    var postPrefilled: Boolean = false
    val createOrEditPostResult = StatefulLiveData<PostView>()

    fun createPost(
        instance: String,
        name: String,
        body: String,
        url: String,
        isNsfw: Boolean,
        communityNameOrId: Either<String, CommunityId>,
    ) {
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

    class NoTitleError : RuntimeException()
}