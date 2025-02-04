package com.idunnololz.summit.actions

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.PendingActionsManager.ActionExecutionResult.Failure
import com.idunnololz.summit.api.ApiException
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.NetworkException
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.ServerApiException
import com.idunnololz.summit.api.SocketTimeoutException
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.RateLimitManager
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason.AccountNotFoundError
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason.RateLimit
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason.TooManyRequests
import com.idunnololz.summit.lemmy.actions.LemmyActionResult
import com.idunnololz.summit.lemmy.utils.VotableRef
import javax.inject.Inject

class ActionsRunnerHelper @Inject constructor(
    private val accountManager: AccountManager,
    private val apiClient: LemmyApiClient,
) {

    companion object {
        private const val TAG = "ActionsRunnerHelper"
    }

    suspend fun executeAction(
        actionInfo: ActionInfo,
        retries: Int,
    ): PendingActionsManager.ActionExecutionResult {
        fun getResultForError(error: Throwable): PendingActionsManager.ActionExecutionResult =
            when (error) {
                is ApiException -> {
                    when (error) {
                        is ClientApiException -> {
                            if (error is NotAuthenticatedException) {
                                Failure(AccountNotFoundError(actionInfo.accountId ?: 0))
                            }
                            if (error.errorCode == 429) {
                                if (RateLimitManager.isRateLimitHit()) {
                                    Log.d(TAG, "429. Hard limit hit. Rescheduling action...")

                                    val nextRefresh = RateLimitManager.getTimeUntilNextRefreshMs()
                                    Failure(RateLimit(recommendedTimeoutMs = nextRefresh))
                                } else {
                                    Failure(TooManyRequests(retries + 1))
                                }
                            } else {
                                Failure(LemmyActionFailureReason.UnknownError(error.errorCode, ""))
                            }
                        }
                        is ServerApiException ->
                            Failure(LemmyActionFailureReason.ServerError)
                    }
                }
                is SocketTimeoutException ->
                    Failure(LemmyActionFailureReason.ConnectionError)
                is NetworkException ->
                    Failure(LemmyActionFailureReason.ConnectionError)
                else -> {
                    Failure(LemmyActionFailureReason.UnknownError(-1, error.javaClass.simpleName))
                }
            }

        val accountId = actionInfo.accountId
        val account = if (accountId != null) {
            accountManager.getAccountById(accountId)
        } else {
            null
        }

        if (account != null) {
            apiClient.changeInstance(account.instance)
        }

        when (actionInfo) {
            is ActionInfo.VoteActionInfo -> {
                if (account == null) {
                    return Failure(AccountNotFoundError(actionInfo.accountId))
                }

                val result: Result<Either<PostView, CommentView>> = when (actionInfo.ref) {
                    is VotableRef.CommentRef -> {
                        apiClient.likeCommentWithRetry(
                            actionInfo.ref.commentId,
                            actionInfo.dir,
                            account,
                        )
                            .fold(
                                onSuccess = {
                                    Result.success(Either.Right(it))
                                },
                                onFailure = {
                                    Result.failure(it)
                                },
                            )
                    }
                    is VotableRef.PostRef -> {
                        apiClient.likePostWithRetry(
                            actionInfo.ref.postId,
                            actionInfo.dir,
                            account,
                        )
                            .fold(
                                onSuccess = {
                                    Result.success(Either.Left(it))
                                },
                                onFailure = {
                                    Result.failure(it)
                                },
                            )
                    }
                }

                return result.fold(
                    onSuccess = {
                        PendingActionsManager.ActionExecutionResult.Success(
                            LemmyActionResult.VoteLemmyActionResult(it),
                        )
                    },
                    onFailure = {
                        getResultForError(it)
                    },
                )
            }
            is ActionInfo.CommentActionInfo -> {
                if (account == null) {
                    return Failure(AccountNotFoundError(actionInfo.accountId))
                }

                val result = apiClient.createComment(
                    account = account,
                    content = actionInfo.content,
                    postId = actionInfo.postRef.id,
                    parentId = actionInfo.parentId,
                )

                return result.fold(
                    onSuccess = {
                        PendingActionsManager.ActionExecutionResult.Success(
                            LemmyActionResult.CommentLemmyActionResult(),
                        )
                    },
                    onFailure = {
                        getResultForError(it)
                    },
                )
            }
            is ActionInfo.EditCommentActionInfo -> {
                if (account == null) {
                    return Failure(AccountNotFoundError(actionInfo.accountId))
                }

                val result = apiClient.editComment(
                    account = account,
                    content = actionInfo.content,
                    commentId = actionInfo.commentId,
                )

                return result.fold(
                    onSuccess = {
                        PendingActionsManager.ActionExecutionResult.Success(
                            LemmyActionResult.EditLemmyActionResult(),
                        )
                    },
                    onFailure = {
                        getResultForError(it)
                    },
                )
            }
            is ActionInfo.DeleteCommentActionInfo -> {
                if (account == null) {
                    return Failure(AccountNotFoundError(actionInfo.accountId))
                }

                val result = apiClient.deleteComment(
                    account = account,
                    commentId = actionInfo.commentId,
                )

                return result.fold(
                    onSuccess = {
                        PendingActionsManager.ActionExecutionResult.Success(
                            LemmyActionResult.DeleteCommentLemmyActionResult(),
                        )
                    },
                    onFailure = {
                        getResultForError(it)
                    },
                )
            }
            is ActionInfo.MarkPostAsReadActionInfo -> {
                if (account == null) {
                    return Failure(AccountNotFoundError(actionInfo.accountId))
                }

                val result = apiClient.markPostAsRead(
                    actionInfo.postRef.id,
                    actionInfo.read,
                    account,
                )

                return result.fold(
                    onSuccess = {
                        PendingActionsManager.ActionExecutionResult.Success(
                            LemmyActionResult.MarkPostAsReadActionResult(),
                        )
                    },
                    onFailure = {
                        getResultForError(it)
                    },
                )
            }
        }
    }
}