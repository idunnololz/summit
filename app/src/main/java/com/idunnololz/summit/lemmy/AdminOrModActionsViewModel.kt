package com.idunnololz.summit.lemmy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.AccountInstanceMismatchException
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PostFeatureType
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AdminOrModActionsViewModel @Inject constructor(
    var apiClient: AccountAwareLemmyClient,
    val accountManager: AccountManager,
    val accountInfoManager: AccountInfoManager,
    val accountActionsManager: AccountActionsManager,
) : ViewModel() {

    val currentAccount: Account?
        get() = apiClient.accountForInstance()
    val apiInstance: String
        get() = apiClient.instance

    val banUserResult = StatefulLiveData<Unit>()
    val modUserResult = StatefulLiveData<Unit>()
    val distinguishCommentResult = StatefulLiveData<Unit>()
    val removeCommentResult = StatefulLiveData<Unit>()

    val banUserFromSiteResult = StatefulLiveData<Unit>()
    val removeCommunityResult = StatefulLiveData<Unit>()
    val hideCommunityResult = StatefulLiveData<Unit>()

    val purgeCommunityResult = StatefulLiveData<Unit>()
    val purgePostResult = StatefulLiveData<Unit>()
    val purgeUserResult = StatefulLiveData<Unit>()
    val purgeCommentResult = StatefulLiveData<Unit>()

    val featurePostResult = StatefulLiveData<PostView>()
    val lockPostResult = StatefulLiveData<PostView>()
    val removePostResult = StatefulLiveData<PostView>()

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
            apiClient.banUserFromCommunity(
                communityId,
                personId,
                ban,
                removeData,
                reason,
                expiresDays,
            )
                .onSuccess {
                    banUserResult.postValue(Unit)
                }
                .onFailure {
                    banUserResult.postError(it)
                }
        }
    }

    fun featurePost(postId: PostId, feature: Boolean) {
        featurePostResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.featurePost(postId, feature, PostFeatureType.Community)
            }
                .onSuccess {
                    featurePostResult.postValue(it)
                }
                .onFailure {
                    featurePostResult.postError(it)
                }
        }
    }

    fun removePost(postId: PostId, remove: Boolean, reason: String?) {
        removePostResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.removePost(postId, remove, reason)
            }
                .onSuccess {
                    removePostResult.postValue(it)
                }
                .onFailure {
                    removePostResult.postError(it)
                }
        }
    }

    fun removeComment(commentId: Int, remove: Boolean, reason: String?) {
        removeCommentResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.removeComment(
                    commentId,
                    remove,
                    reason,
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

    fun lockPost(postId: PostId, lock: Boolean) {
        lockPostResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.lockPost(postId, lock)
            }
                .onSuccess {
                    lockPostResult.postValue(it)
                }
                .onFailure {
                    lockPostResult.postError(it)
                }
        }
    }

    fun mod(communityId: Int, personId: PersonId, mod: Boolean) {
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

    fun distinguishComment(commentId: CommentId, distinguish: Boolean) {
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

    fun banUserFromSite(
        personId: PersonId,
        ban: Boolean,
        removeData: Boolean,
        reason: String?,
        expiresDays: Int?,
    ) {
        banUserFromSiteResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.banUserFromSite(
                    personId,
                    ban,
                    removeData,
                    reason,
                    expiresDays,
                )
            }
                .onSuccess {
                    banUserFromSiteResult.postValue(Unit)
                }
                .onFailure {
                    banUserFromSiteResult.postError(it)
                }
        }
    }

    fun removeCommunity(communityId: CommunityId, remove: Boolean, reason: String?) {
        removeCommunityResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.removeCommunity(
                    communityId = communityId,
                    remove = remove,
                    reason = reason,
                )
            }
                .onSuccess {
                    removeCommunityResult.postValue(Unit)
                }
                .onFailure {
                    removeCommunityResult.postError(it)
                }
        }
    }

    fun hideCommunity(communityId: CommunityId, hide: Boolean, reason: String?) {
        hideCommunityResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.hideCommunity(
                    communityId = communityId,
                    hide = hide,
                    reason = reason,
                )
            }
                .onSuccess {
                    hideCommunityResult.postValue(Unit)
                }
                .onFailure {
                    hideCommunityResult.postError(it)
                }
        }
    }

    fun purgeCommunity(communityId: CommunityId, reason: String?) {
        purgeCommunityResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.purgeCommunity(
                    communityId = communityId,
                    reason = reason,
                )
            }
                .onSuccess {
                    purgeCommunityResult.postValue(Unit)
                }
                .onFailure {
                    purgeCommunityResult.postError(it)
                }
        }
    }

    fun purgePost(postId: PostId, reason: String?) {
        purgePostResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.purgePost(
                    postId = postId,
                    reason = reason,
                )
            }
                .onSuccess {
                    purgePostResult.postValue(Unit)
                }
                .onFailure {
                    purgePostResult.postError(it)
                }
        }
    }

    fun purgePerson(personId: PersonId, reason: String?) {
        purgeUserResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.purgePerson(
                    personId = personId,
                    reason = reason,
                )
            }
                .onSuccess {
                    purgeUserResult.postValue(Unit)
                }
                .onFailure {
                    purgeUserResult.postError(it)
                }
        }
    }

    fun purgeComment(commentId: CommentId, reason: String?) {
        purgeCommentResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance {
                apiClient.purgeComment(
                    commentId = commentId,
                    reason = reason,
                )
            }
                .onSuccess {
                    purgeCommentResult.postValue(Unit)
                }
                .onFailure {
                    purgeCommentResult.postError(it)
                }
        }
    }

    private suspend fun <T> ensureRightInstance(
        onCorrectInstance: suspend () -> Result<T>,
    ): Result<T> {
        val currentInstance = apiInstance
        val currentAccount = currentAccount
        return if (currentAccount == null) {
            Result.failure(
                NotAuthenticatedException(),
            )
        } else if (currentInstance != currentAccount.instance) {
            Result.failure(
                AccountInstanceMismatchException(
                    apiInstance,
                    currentAccount.instance,
                ),
            )
        } else {
            onCorrectInstance()
        }
    }
}
