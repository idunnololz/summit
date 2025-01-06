package com.idunnololz.summit.lemmy.mod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@HiltViewModel
class ModActionsViewModel @Inject constructor(
    private val accountInfoManager: AccountInfoManager,
    private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {

    sealed interface ModState {
        data class CommentModState(
            val isRemoved: Boolean,
            val isDistinguished: Boolean?,
        ) : ModState
        data class PostModState(
            val isRemoved: Boolean,
            val isFeatured: Boolean,
            val isLocked: Boolean,
        ) : ModState
        data class UserModState(
            val isBannedFromCommunity: Boolean,
            val isBannedFromSite: Boolean,
        ) : ModState
        data class CommunityModState(
            val isMod: Boolean,
            val isRemoved: Boolean,
            val isHidden: Boolean,
        ) : ModState
    }

    data class FullModState(
        val modStates: List<ModState>,
    )

    val currentAccount
        get() = apiClient.accountForInstance()
    val currentModState = StatefulLiveData<FullModState>()

    fun isAdmin(communityInstance: String): Boolean {
        val fullAccount = accountInfoManager.currentFullAccount.value
        val miscAccountInfo = fullAccount
            ?.accountInfo
            ?.miscAccountInfo
        return fullAccount?.account?.instance == communityInstance &&
            miscAccountInfo?.isAdmin == true
    }

    fun loadModActionsState(
        communityId: Int,
        postId: Int,
        commentId: Int,
        personId: PersonId,
        force: Boolean = false,
    ) {
        currentModState.setIsLoading()

        viewModelScope.launch {
            val communityJob = async {
                apiClient.fetchCommunityWithRetry(Either.Left(communityId), force)
                    .onFailure {
                        currentModState.postError(it)
                        cancel()
                    }
            }
            val postJob = if (postId != -1) {
                async {
                    apiClient.fetchPostWithRetry(Either.Left(postId), force)
                        .onFailure {
                            currentModState.postError(it)
                            cancel()
                        }
                }
            } else {
                null
            }
            val commentJob = if (commentId != -1) {
                async {
                    apiClient
                        .fetchCommentsWithRetry(
                            id = Either.Right(commentId),
                            sort = CommentSortType.New,
                            force = force,
                            maxDepth = 1,
                        )
                        .onFailure {
                            currentModState.postError(it)
                            cancel()
                        }
                }
            } else {
                null
            }

            val allModState = mutableListOf<ModState>()

            // Do all or nothing for the API calls since it's easier to handle.
            val communityResult = communityJob.await().getOrNull()
            val postResult = postJob?.await()?.getOrNull()
            val commentResult = commentJob?.await()?.getOrNull()

            ensureActive()
            if (postResult != null) {
                allModState +=
                    ModState.PostModState(
                        isRemoved = postResult.post.removed,
                        isFeatured = postResult.post.featured_community,
                        isLocked = postResult.post.locked,
                    )
                allModState +=
                    ModState.UserModState(
                        isBannedFromCommunity = postResult.creator_banned_from_community,
                        isBannedFromSite = postResult.creator.banned,
                    )
            }
            if (commentResult != null) {
                val comment = commentResult.firstOrNull { it.comment.id == commentId }
                if (comment == null) {
                    allModState +=
                        ModState.CommentModState(
                            isRemoved = true,
                            isDistinguished = null,
                        )
                } else {
                    allModState +=
                        ModState.CommentModState(
                            isRemoved = comment.comment.removed,
                            isDistinguished = comment.comment.distinguished,
                        )
                    allModState +=
                        ModState.UserModState(
                            isBannedFromCommunity = comment.creator_banned_from_community,
                            isBannedFromSite = comment.creator.banned,
                        )
                }
            }

            if (communityResult != null) {
                allModState +=
                    ModState.CommunityModState(
                        isMod = communityResult.moderators.any { it.moderator.id == personId },
                        isRemoved = communityResult.community_view.community.removed,
                        isHidden = communityResult.community_view.community.hidden,
                    )
            }

            currentModState.postValue(FullModState(allModState))
        }
    }
}
