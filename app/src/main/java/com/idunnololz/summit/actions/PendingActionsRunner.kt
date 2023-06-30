package com.idunnololz.summit.actions

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import arrow.core.Either
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.PendingActionsManager.ActionExecutionResult.Failure
import com.idunnololz.summit.api.ApiException
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.ServerApiException
import com.idunnololz.summit.api.SocketTimeoutException
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.connectivity.ConnectivityChangedWorker
import com.idunnololz.summit.lemmy.RateLimitManager
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyActionResult
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason.AccountNotFoundError
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason.RateLimit
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason.TooManyRequests
import com.idunnololz.summit.lemmy.utils.VotableRef
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList
import kotlin.coroutines.CoroutineContext
import kotlin.math.pow

class PendingActionsRunner @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    private val accountManager: AccountManager,
    private val apiClient: LemmyApiClient,

    @Assisted private val actions: LinkedList<LemmyAction>,
    @Assisted private val coroutineScope: CoroutineScope,
    @Assisted private val actionsContext: CoroutineContext,

    @Assisted private val delayAction: suspend (action: LemmyAction, nextRefreshMs: Long) -> Unit,
    @Assisted private val completeActionError: suspend (action: LemmyAction, failureReason: LemmyActionFailureReason) -> Unit,
    @Assisted private val completeActionSuccess: suspend (action: LemmyAction, result: LemmyActionResult<*, *>) -> Unit,
) {
    @AssistedFactory
    interface Factory {
        fun create(
            actions: LinkedList<LemmyAction>,
            coroutineScope: CoroutineScope,
            actionsContext: CoroutineContext,

            delayAction: suspend (action: LemmyAction, nextRefreshMs: Long) -> Unit,
            completeActionError: suspend (action: LemmyAction, failureReason: LemmyActionFailureReason) -> Unit,
            completeActionSuccess: suspend (action: LemmyAction, result: LemmyActionResult<*, *>) -> Unit,
        ): PendingActionsRunner
    }

    companion object {
        private const val TAG = "PendingActionsRunner"
    }

    private val lock = Object()
    private var isExecutingPendingActions = false
    private var connectionIssue = false

    fun executePendingActionsIfNeeded() {
        synchronized(lock) {
            if (isExecutingPendingActions) {
                return
            }
        }

        executePendingActions()
    }

    private fun executePendingActions() {
        isExecutingPendingActions = true
        connectionIssue = false

        coroutineScope.launch a@{
            val newIsExecutingPendingActions = withContext(actionsContext) {
                actions.isNotEmpty()
            }

            synchronized(lock) {
                isExecutingPendingActions = newIsExecutingPendingActions

                if (!isExecutingPendingActions) {
                    Log.d(TAG, "All pending actions executed.")
                    return@a
                }
            }

            val action = withContext(actionsContext) {
                actions.removeFirst()
            }

            checkAndExecuteAction(action)

            if (connectionIssue) {
                Log.d(TAG, "No internet. Deferring execution until later.")
                actions.addFirst(action)
                synchronized(lock) {
                    isExecutingPendingActions = false
                }
                scheduleWaitForConnectionWorker()
            } else {
                Log.d(TAG, "Executing more pending actions")
                executePendingActions()
            }
        }
    }

    private suspend fun checkAndExecuteAction(action: LemmyAction) {
        if (action.info == null) {
            completeActionError(action, LemmyActionFailureReason.DeserializationError)
            return
        } else if (action.info.isAffectedByRateLimit && RateLimitManager.isRateLimitHit()) {
            Log.d(TAG, "Delaying pending action $action")
            val nextRefresh = RateLimitManager.getTimeUntilNextRefreshMs()
            delayAction(action, nextRefresh)
            return
        }

        try {
            Log.d(TAG, "Executing action $action")
            val result = executeAction(action, action.info.retries)

            when (result) {
                is PendingActionsManager.ActionExecutionResult.Success -> {
                    completeActionSuccess(action, result.result)
                }
                is Failure -> {
                    when (result.failureReason) {
                        is AccountNotFoundError ->
                            completeActionError(
                                action,
                                AccountNotFoundError(
                                    result.failureReason.accountId)
                            )
                        LemmyActionFailureReason.ConnectionError ->
                            connectionIssue = true
                        is RateLimit ->
                            delayAction(action, result.failureReason.recommendedTimeoutMs)
                        is TooManyRequests ->
                            if (result.failureReason.retries < PendingActionsManager.MAX_RETRIES) {
                                // Prob just sending too fast...
                                val delay = (2.0.pow(result.failureReason.retries + 1) * 1000).toLong()
                                Log.d(TAG, "429. Retrying after ${delay}...")


                                delayAction(action, delay)
                            } else {
                                Log.e(
                                    TAG,
                                    "Request failed. Too many retries. ",
                                    RuntimeException()
                                )

                                completeActionError(action, result.failureReason)
                            }
                        LemmyActionFailureReason.ServerError ->
                            delayAction(action, 10_000)

                        is LemmyActionFailureReason.UnknownError,
                        LemmyActionFailureReason.DeserializationError,
                        LemmyActionFailureReason.ActionOverwritten ->
                            completeActionError(action, result.failureReason)
                    }
                }
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Log.e(TAG, "Error executing pending action.", e)
        }
    }

    private suspend fun executeAction(
        action: LemmyAction,
        retries: Int
    ): PendingActionsManager.ActionExecutionResult {
        var shouldDeleteAction = true

        fun getResultForError(error: Throwable): PendingActionsManager.ActionExecutionResult =
            when (error) {
                is ApiException -> {
                    when (error) {
                        is ClientApiException -> {
                            if (error.errorCode == 429) {
                                if (RateLimitManager.isRateLimitHit()) {
                                    shouldDeleteAction = false

                                    Log.d(TAG, "429. Hard limit hit. Rescheduling action...")

                                    val nextRefresh = RateLimitManager.getTimeUntilNextRefreshMs()
                                    Failure(RateLimit(recommendedTimeoutMs = nextRefresh))
                                } else {
                                    Failure(TooManyRequests(retries + 1))
                                }
                            } else {
                                Failure(LemmyActionFailureReason.UnknownError(0))
                            }
                        }
                        is ServerApiException ->
                            Failure(LemmyActionFailureReason.ServerError)
                    }
                }
                is SocketTimeoutException ->
                    Failure(LemmyActionFailureReason.ServerError)
                else -> {
                    Failure(LemmyActionFailureReason.UnknownError(0))
                }
            }

        val actionInfo = requireNotNull(action.info)

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
                            actionInfo.ref.commentId, actionInfo.dir, account)
                            .fold(
                                onSuccess = {
                                    Result.success(Either.Right(it))
                                },
                                onFailure = {
                                    Result.failure(it)
                                }
                            )
                    }
                    is VotableRef.PostRef -> {
                        apiClient.likePostWithRetry(
                            actionInfo.ref.postId, actionInfo.dir, account)
                            .fold(
                                onSuccess = {
                                    Result.success(Either.Left(it))
                                },
                                onFailure = {
                                    Result.failure(it)
                                }
                            )
                    }
                }

                return result.fold(
                    onSuccess = {
                        PendingActionsManager.ActionExecutionResult.Success(
                            LemmyActionResult.VoteLemmyActionResult(it))
                    },
                    onFailure = {
                        getResultForError(it)
                    }
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
                    parentId = actionInfo.parentId
                )

                return result.fold(
                    onSuccess = {
                        PendingActionsManager.ActionExecutionResult.Success(
                            LemmyActionResult.CommentLemmyActionResult())
                    },
                    onFailure = {
                        getResultForError(it)
                    }
                )
            }
            is ActionInfo.EditActionInfo -> {
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
                            LemmyActionResult.EditLemmyActionResult())
                    },
                    onFailure = {
                        getResultForError(it)
                    }
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
                            LemmyActionResult.DeleteCommentLemmyActionResult())
                    },
                    onFailure = {
                        getResultForError(it)
                    }
                )
            }
            else -> throw RuntimeException("Unknown action type ${action.info}")
        }
    }

    private fun scheduleWaitForConnectionWorker() {
        val workRequest = OneTimeWorkRequestBuilder<ConnectivityChangedWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}