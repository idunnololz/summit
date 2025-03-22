package com.idunnololz.summit.actions

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.idunnololz.summit.actions.PendingActionsManager.ActionExecutionResult.Failure
import com.idunnololz.summit.connectivity.ConnectivityChangedWorker
import com.idunnololz.summit.lemmy.RateLimitManager
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason.AccountNotFoundError
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason.RateLimit
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason.TooManyRequests
import com.idunnololz.summit.lemmy.actions.LemmyActionResult
import com.idunnololz.summit.lemmy.actions.LemmyPendingAction
import com.idunnololz.summit.util.crashLogger.crashLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.LinkedList
import kotlin.coroutines.CoroutineContext
import kotlin.math.pow
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PendingActionsRunner @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    private val actionsRunnerHelper: ActionsRunnerHelper,

    @Assisted private val actions: LinkedList<LemmyPendingAction>,
    @Assisted private val coroutineScope: CoroutineScope,
    @Assisted private val actionsContext: CoroutineContext,

    @Assisted private val delayAction:
    suspend (action: LemmyPendingAction, nextRefreshMs: Long) -> Unit,
    @Assisted private val completeActionError: suspend (
        action: LemmyPendingAction,
        failureReason: LemmyActionFailureReason,
    ) -> Unit,
    @Assisted private val completeActionSuccess: suspend (
        action: LemmyPendingAction,
        result: LemmyActionResult<*, *>,
    ) -> Unit,
) {
    @AssistedFactory
    interface Factory {
        fun create(
            actions: LinkedList<LemmyPendingAction>,
            coroutineScope: CoroutineScope,
            actionsContext: CoroutineContext,
            delayAction: suspend (action: LemmyPendingAction, nextRefreshMs: Long) -> Unit,
            completeActionError: suspend (
                action: LemmyPendingAction,
                failureReason: LemmyActionFailureReason,
            ) -> Unit,
            completeActionSuccess: suspend (
                action: LemmyPendingAction,
                result: LemmyActionResult<*, *>,
            ) -> Unit,
        ): PendingActionsRunner
    }

    companion object {
        private const val TAG = "PendingActionsRunner"
    }

    private val lock = Object()
    private var isExecutingPendingActions = false
    private var connectionIssue = false

    private var executePendingActionsJob: Job? = null

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

        executePendingActionsJob = coroutineScope.launch a@{
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

    private suspend fun checkAndExecuteAction(action: LemmyPendingAction) {
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
            val result = actionsRunnerHelper.executeAction(action.info, action.info.retries)

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
                                    result.failureReason.accountId,
                                ),
                            )
                        LemmyActionFailureReason.NoInternetError ->
                            connectionIssue = true
                        is RateLimit ->
                            delayAction(action, result.failureReason.recommendedTimeoutMs)
                        is TooManyRequests ->
                            if (result.failureReason.retries < PendingActionsManager.MAX_RETRIES) {
                                // Prob just sending too fast...
                                val delay =
                                    (2.0.pow(result.failureReason.retries + 1) * 1000).toLong() +
                                        (Random.nextFloat() * 2000).toInt()
                                Log.d(TAG, "429. Retrying after $delay...")

                                delayAction(action, delay)
                            } else {
                                Log.e(
                                    TAG,
                                    "Request failed. Too many retries. ",
                                    RuntimeException(),
                                )

                                completeActionError(action, result.failureReason)
                            }
                        LemmyActionFailureReason.ServerError ->
                            delayAction(action, 10_000)

                        is LemmyActionFailureReason.ConnectionError -> {
                            if (action.info is ActionInfo.CommentActionInfo) {
                                completeActionError(action, result.failureReason)
                            } else {
                                delayAction(action, 10_000)
                            }
                        }

                        is LemmyActionFailureReason.UnknownError,
                        LemmyActionFailureReason.DeserializationError,
                        LemmyActionFailureReason.ActionOverwritten,
                        ->
                            completeActionError(action, result.failureReason)
                    }
                }
            }
        } catch (e: Exception) {
            crashLogger?.recordException(e)
            Log.e(TAG, "Error executing pending action.", e)
        }
    }

    private fun scheduleWaitForConnectionWorker() {
        val workRequest = OneTimeWorkRequestBuilder<ConnectivityChangedWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun stopPendingActions() {
        executePendingActionsJob?.cancel()
    }
}
