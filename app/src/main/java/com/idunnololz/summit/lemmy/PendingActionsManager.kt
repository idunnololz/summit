package com.idunnololz.summit.lemmy

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.ApiException
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.ServerApiException
import com.idunnololz.summit.connectivity.ConnectivityChangedWorker
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.db.MainDatabase
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class PendingActionsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val accountManager: AccountManager,
    private val apiClient: LemmyApiClient,
) {

    companion object {

        private const val TAG = "PendingActionsManager"

        const val MAX_RETRIES = 3
    }

    interface OnActionChangedListener {
        fun onActionAdded(action: LemmyAction)
        fun onActionFailed(action: LemmyAction, reason: Throwable)
        fun onActionComplete(action: LemmyAction)
    }

    /**
     * Modifications to [actions] must be made with the [actionsContext].
     */
    private val actions = LinkedList<LemmyAction>()
    private val actionsContext = newSingleThreadContext("CounterContext")

    private var actionDao = MainDatabase.getInstance(context).lemmyActionsDao()

    private val onActionChangedListeners = arrayListOf<OnActionChangedListener>()

    private val defaultErrorHandler = Consumer<Throwable> {
        Log.e(TAG, "", it)
    }

    private var isExecutingPendingActions: Boolean = false

    private val coroutineScope = coroutineScopeFactory.create()

    init {
        coroutineScope.launch {
            val dbActions = actionDao.getAllPendingActions()

            withContext(actionsContext) {
                actions.clear()
                actions.addAll(dbActions)
                actions.sortedBy { it.ts }
            }

            for (action in actions) {
                Log.d(TAG, "Restored action with id ${action.id}")
                notifyActionAdded(action)
            }
        }

        executePendingActionsIfNeeded()
    }

    fun executePendingActionsIfNeeded() {
        synchronized(this@PendingActionsManager) {
            if (isExecutingPendingActions) {
                return
            }
        }

        executePendingActions()
    }

    fun getAllPendingActions(): List<LemmyAction> = actions

    inline fun <reified T : ActionInfo> getPendingActionInfo(): List<T> =
        getAllPendingActions().map { it.info }.filterIsInstance<T>()

    /**
     * @param dir 1 for like, 0 for unvote, -1 for dislike
     */
    fun voteOn(
        instance: String,
        ref: VotableRef,
        dir: Int,
        accountId: Int,
    ) {
        coroutineScope.launch {
            withContext(actionsContext) {
                val iterator = actions.iterator()
                while (iterator.hasNext()) {
                    val action = iterator.next()

                    if (action.info is ActionInfo.VoteActionInfo) {
                        if (action.info.ref == ref) {
                            iterator.remove()
                            actionDao.delete(action)

                            notifyActionFailed(action,
                                PendingActionsException.ActionRemovedException()
                            )
                        }
                    }
                }
            }

            insertAction(
                LemmyAction(
                    id = 0,
                    ts = System.currentTimeMillis(),
                    creationTs = System.currentTimeMillis(),
                    info = ActionInfo.VoteActionInfo(instance, ref, dir, 2, accountId)
                )
            )
        }
    }

    /**
     * Posts a comment/reply to another comment.
     * @param parentId what to comment on
     * @param text text to post
     */
    fun comment(
        parentId: String,
        text: String,
        lifecycleOwner: LifecycleOwner,
        cb: (StatefulData<LemmyAction>) -> Unit
    ) {
//        Single
//            .create<RedditAction> { emitter ->
//                val action = insertAction(
//                    RedditAction(
//                        id = 0,
//                        ts = System.currentTimeMillis(),
//                        creationTs = System.currentTimeMillis(),
//                        info = ActionInfo.CommentActionInfo(parentId = parentId, text = text)
//                    )
//                )
//
//                createLiveDataAndRegisterCallback(action, lifecycleOwner, cb)
//
//                emitter.onSuccess(action)
//            }
//            .subscribeOn(scheduler)
//            .subscribeWithDefaultErrorHandler {}
    }

    /**
     * Edit the body text of a comment or self-post.
     * @param thingId what to edit
     * @param text text to post
     */
    fun edit(
        thingId: String,
        text: String,
        lifecycleOwner: LifecycleOwner,
        cb: (StatefulData<LemmyAction>) -> Unit
    ) {
//        Single
//            .create<RedditAction> { emitter ->
//                val action = insertAction(
//                    RedditAction(
//                        id = 0,
//                        ts = System.currentTimeMillis(),
//                        creationTs = System.currentTimeMillis(),
//                        info = ActionInfo.EditActionInfo(thingId = thingId, text = text)
//                    )
//                )
//
//                createLiveDataAndRegisterCallback(action, lifecycleOwner, cb)
//
//                emitter.onSuccess(action)
//            }
//            .subscribeOn(scheduler)
//            .subscribeWithDefaultErrorHandler {}
    }

    fun deleteComment(
        commentId: String,
        lifecycleOwner: LifecycleOwner,
        cb: (StatefulData<LemmyAction>) -> Unit
    ) {
//        Single
//            .create<RedditAction> { emitter ->
//                val action = insertAction(
//                    RedditAction(
//                        id = 0,
//                        ts = System.currentTimeMillis(),
//                        creationTs = System.currentTimeMillis(),
//                        info = ActionInfo.DeleteCommentActionInfo(id = commentId)
//                    )
//                )
//
//                createLiveDataAndRegisterCallback(action, lifecycleOwner, cb)
//
//                emitter.onSuccess(action)
//            }
//            .subscribeOn(scheduler)
//            .subscribeWithDefaultErrorHandler {}
    }

    fun addActionCompleteListener(l: OnActionChangedListener) {
        onActionChangedListeners.add(l)
    }

    fun removeActionCompleteListener(l: OnActionChangedListener) {
        onActionChangedListeners.remove(l)
    }

    private fun notifyActionComplete(action: LemmyAction) {
        for (onActionCompleteListener in onActionChangedListeners) {
            onActionCompleteListener.onActionComplete(action)
        }
    }

    private fun notifyActionAdded(action: LemmyAction) {
        for (onActionCompleteListener in onActionChangedListeners) {
            onActionCompleteListener.onActionAdded(action)
        }
    }

    private fun notifyActionFailed(action: LemmyAction, reason: Throwable) {
        for (onActionCompleteListener in onActionChangedListeners) {
            onActionCompleteListener.onActionFailed(action, reason)
        }
    }

    private fun executePendingActions() {
        isExecutingPendingActions = true

        coroutineScope.launch a@{
            val newIsExecutingPendingActions = withContext(actionsContext) {
                actions.isNotEmpty()
            }

            synchronized(this@PendingActionsManager) {
                isExecutingPendingActions = newIsExecutingPendingActions

                if (!isExecutingPendingActions) {
                    Log.d(TAG, "All pending actions executed.")
                    return@a
                }
            }

            var connectionIssue = false

            val action = withContext(actionsContext) {
                actions.removeFirst()
            }

            suspend fun completeActionError(action: LemmyAction, error: Throwable) {
                actionDao.delete(action)
                notifyActionFailed(action, error)
            }

            suspend fun completeActionSuccess(action: LemmyAction) {
                actionDao.delete(action)
                notifyActionComplete(action)
            }

            if (action.info == null) {
                completeActionError(action,
                    PendingActionsException.ActionDeserializationException()
                )
            } else if (action.info.isAffectedByRateLimit && RateLimitManager.isRateLimitHit()) {
                Log.d(TAG, "Delaying pending action $action")
                val nextRefresh = RateLimitManager.getTimeUntilNextRefreshMs()
                delayAction(action, nextRefresh)
            } else {
                try {
                    Log.d(TAG, "Executing action $action")
                    val result = executeAction(action, action.info.retries)

                    when (result) {
                        ActionExecutionResult.Success -> {
                            completeActionSuccess(action)
                        }
                        is ActionExecutionResult.RateLimit -> {
                            delayAction(action, result.recommendedTimeoutMs)
                        }
                        is ActionExecutionResult.TooManyRequests -> {
                            if (result.retries < MAX_RETRIES) {
                                // Prob just sending too fast...
                                val delay = (2.0.pow(result.retries + 1) * 1000).toLong()
                                Log.d(TAG, "429. Retrying after ${delay}...")
                                Thread.sleep(delay)

                                withContext(actionsContext) {
                                    actions.add(action.copy(info = action.info.incRetries()))
                                }
                            } else {
                                Log.e(
                                    TAG,
                                    "Request failed. Too many retries. ",
                                    RuntimeException()
                                )

                                completeActionError(action, PendingActionsException.RetriesExceededException(RuntimeException()))
                            }
                        }
                        is ActionExecutionResult.UnknownError -> {
                            completeActionError(action, RuntimeException())
                        }
                        ActionExecutionResult.ConnectionError -> {
                            connectionIssue = true
                        }

                        is ActionExecutionResult.AccountNotFoundError ->
                            completeActionError(
                                action = action,
                                error = PendingActionsException.AccountNotFoundException(result.accountId)
                            )
                    }
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                    Log.e(TAG, "Error executing pending action.", e)
                }
            }

            if (connectionIssue) {
                Log.d(TAG, "No internet. Deferring execution until later.")
                actions.addFirst(action)
                synchronized(this@PendingActionsManager) {
                    isExecutingPendingActions = false
                }
                scheduleWaitForConnectionWorker()
            } else {
                Log.d(TAG, "Executing more pending actions")
                executePendingActions()
            }
        }

    }

    private fun delayAction(action: LemmyAction, nextRefreshMs: Long) {
        coroutineScope.launch (Dispatchers.IO) {
            delay(nextRefreshMs)

            insertAction(action.copy(ts = System.currentTimeMillis()))
        }
    }

    private suspend fun executeAction(action: LemmyAction, retries: Int): ActionExecutionResult {
        var shouldDeleteAction = true

        fun getResultForError(error: Throwable): ActionExecutionResult =
            when (error) {
                is ApiException -> {
                    when (error) {
                        is ClientApiException -> {
                            if (error.errorCode == 429) {
                                if (RateLimitManager.isRateLimitHit()) {
                                    shouldDeleteAction = false

                                    Log.d(TAG, "429. Hard limit hit. Rescheduling action...")

                                    ActionExecutionResult.RateLimit(RateLimitManager.getTimeUntilNextRefreshMs())
                                } else {
                                    ActionExecutionResult.TooManyRequests(retries + 1, error)
                                }
                            } else {
                                ActionExecutionResult.UnknownError(0)
                            }
                        }
                        is ServerApiException ->
                            ActionExecutionResult.UnknownError(0)
                    }
                }
                else -> {
                    ActionExecutionResult.UnknownError(0)
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
                    return ActionExecutionResult.AccountNotFoundError(actionInfo.accountId)
                }

                val result: Result<Unit> = when (actionInfo.ref) {
                    is VotableRef.CommentRef -> {
                        apiClient.likeCommentWithRetry(
                            actionInfo.ref.commentId, actionInfo.dir, account)
                            .fold(
                                onSuccess = {
                                    Result.success(Unit)
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
                                    Result.success(Unit)
                                },
                                onFailure = {
                                    Result.failure(it)
                                }
                            )
                    }
                }

                return result.fold(
                    onSuccess = {
                        ActionExecutionResult.Success
                    },
                    onFailure = {
                        getResultForError(it)
                    }
                )
            }
            is ActionInfo.CommentActionInfo -> {
                TODO()
//                    val request = RedditAuthManager.instance.makeAuthedPost(
//                        LinkUtils.apiComment(),
//                        FormBody.Builder()
//                            .add("thing_id", action.info.parentId)
//                            .add("text", action.info.text)
//                            .add("api_type", "json")
//                            .add("return_rtjson", "true")
//                            .build()
//                    )
//
//                    val bodyString = request.body?.string()
//                    Log.d(TAG, "Result! Body: $bodyString")
//
//                    if (request.isSuccessful) {
//                        Log.d(TAG, "Success! Body: $bodyString")
//
//                        val item = Utils.gson.fromJson<RedditCommentItem>(
//                            bodyString,
//                            RedditCommentItem::class.java
//                        )
//
//                        PendingCommentsManager.instance.addPendingComment(item)
//
//                        actionIdToLiveData[action.id].postValue(action)
//                        ActionExecutionResult.Success
//                    } else {
//                        getResultForError(request)
//                    }
            }
            is ActionInfo.DeleteCommentActionInfo -> {
                TODO()
//                    val request = RedditAuthManager.instance.makeAuthedPost(
//                        LinkUtils.apiDeleteComment(),
//                        FormBody.Builder()
//                            .add("id", action.info.id)
//                            .build()
//                    )
//
//                    val bodyString = request.body?.string()
//                    Log.d(TAG, "Result! Body: $bodyString")
//
//                    if (request.isSuccessful) {
//                        Log.d(TAG, "Success! Body: $bodyString")
//
//                        actionIdToLiveData[action.id].postValue(action)
//                        ActionExecutionResult.Success
//                    } else {
//                        getResultForError(request)
//                    }
            }
            is ActionInfo.EditActionInfo -> {
                TODO()
//                    val request = RedditAuthManager.instance.makeAuthedPost(
//                        LinkUtils.apiEditUserText(),
//                        FormBody.Builder()
//                            .add("api_type", "json")
//                            .add("return_rtjson", "true")
//                            .add("thing_id", action.info.thingId)
//                            .add("text", action.info.text)
//                            .build()
//                    )
//
//                    val bodyString = request.body?.string()
//                    Log.d(TAG, "Result! Body: $bodyString")
//
//                    if (request.isSuccessful) {
//                        Log.d(TAG, "Success! Body: $bodyString")
//
//                        val item = Utils.gson.fromJson<RedditCommentItem>(
//                            bodyString,
//                            RedditCommentItem::class.java
//                        )
//
//                        PendingEditsManager.instance.addPendingCommentEdit(item)
//
//                        actionIdToLiveData[action.id].postValue(action)
//                        ActionExecutionResult.Success
//                    } else {
//                        getResultForError(request)
//                    }
            }
            else -> throw RuntimeException("Unknown action type ${action.info}")
        }
    }

    private fun <T> Single<T>.subscribeWithDefaultErrorHandler(cb: (T) -> Unit): Disposable =
        subscribe(Consumer<T> { cb(it) }, defaultErrorHandler)

    private suspend fun insertAction(
        action: LemmyAction
    ): LemmyAction = withContext(Dispatchers.Default) {
        val actionId = actionDao.insertAction(action)
        val newAction = action.copy(id = actionId)

        withContext(actionsContext) {
            actions.add(newAction)
        }

        if (action.id == 0L) {
            // this is a new item!
            notifyActionAdded(action)
        }

        executePendingActionsIfNeeded()

        newAction
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

    sealed class ActionExecutionResult {
        object Success : ActionExecutionResult()
        data class RateLimit(
            val recommendedTimeoutMs: Long
        ) : ActionExecutionResult()

        data class TooManyRequests(
            val retries: Int,
            val lastError: Throwable
        ) : ActionExecutionResult()

        data class UnknownError(
            val errorCode: Int
        ) : ActionExecutionResult()

        data class AccountNotFoundError(
            val accountId: Int
        ) : ActionExecutionResult()

        object ConnectionError : ActionExecutionResult()
    }

    sealed class PendingActionsException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause) {
        class ActionRemovedException() :
            PendingActionsException(
                "Pending action was removed due to being overriden by a new conflicting action.")

        class WrappedException(
            val action: LemmyAction,
            cause: Throwable
        ) : PendingActionsException(
            "Error running action ${action}",
            cause
        )

        class AccountNotFoundException(
            val accountId: Int
        ) : PendingActionsException(
            "Could not find the account associated with this action. Account id: $accountId"
        )

        class RetriesExceededException(
            cause: Throwable
        ): PendingActionsException("Retries exceeded.", cause)

        class ActionDeserializationException() : PendingActionsException(
            "There was an error deserializing an action. The action has been removed.")
    }
}