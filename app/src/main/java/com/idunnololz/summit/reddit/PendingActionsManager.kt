package com.idunnololz.summit.reddit

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.LongSparseArray
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.auth.RedditAuthManager
import com.idunnololz.summit.connectivity.ConnectivityChangedWorker
import com.idunnololz.summit.db.MainDatabase
import com.idunnololz.summit.reddit_actions.ActionInfo
import com.idunnololz.summit.reddit_actions.RedditAction
import com.idunnololz.summit.reddit_objects.RedditCommentItem
import com.idunnololz.summit.util.DataWithState
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.Do
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import okhttp3.FormBody
import okhttp3.Response
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class PendingActionsManager(private val context: Context) {

    companion object {

        private val TAG = "PendingActionsManager"

        @SuppressLint("StaticFieldLeak") // application context
        lateinit var instance: PendingActionsManager
            private set

        fun initialize(context: Context) {
            instance = PendingActionsManager(context.applicationContext)
        }

        const val MAX_RETRIES = 3
    }

    interface OnActionChangedListener {
        fun onActionAdded(action: RedditAction)
        fun onActionFailed(action: RedditAction)
        fun onActionComplete(action: RedditAction)
    }

    private val scheduler: Scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    private var actions = LinkedList<RedditAction>()

    private var actionDao = MainDatabase.getInstance(context).redditActionDao()

    private var actionIdToLiveData = LongSparseArray<StatefulLiveData<RedditAction>>()

    private val onActionChangedListeners = arrayListOf<OnActionChangedListener>()

    private val defaultErrorHandler = Consumer<Throwable> {
        Log.e(TAG, "", it)
    }

    private var isExecutingPendingActions: Boolean = false

    init {
        val d = actionDao
            .getAllPendingActions()
            .subscribeOn(scheduler)
            .subscribe({
                actions.clear()
                actions.addAll(it)
                actions.sortedBy { it.ts }

                for (action in actions) {
                    Log.d(TAG, "Restored action with id ${action.id}")
                    actionIdToLiveData.put(action.id, StatefulLiveData())
                    notifyActionAdded(action)
                }
            }, { e ->
                Log.e(TAG, "", e)
            })

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

    fun getAllPendingActions(): List<RedditAction> = actions

    inline fun <reified T : ActionInfo> getPendingActionInfo(): List<T> =
        getAllPendingActions().map { it.info }.filterIsInstance<T>()

    /**
     * @param dir 1 for like, 0 for unvote, -1 for dislike
     */
    fun voteOn(
        id: String,
        dir: Int,
        lifecycleOwner: LifecycleOwner,
        cb: (DataWithState<RedditAction>) -> Unit
    ) {
        Single
            .create<RedditAction> { emitter ->
                // Remove any conflicting pending actions
                actions.retainAll a@{
                    if (it.info is ActionInfo.VoteActionInfo) {
                        if (it.info.id == id) {
                            actionDao.delete(it).blockingAwait()

                            actionIdToLiveData.get(it.id).postError(ActionRemovedException())
                            actionIdToLiveData.remove(it.id)
                            return@a false
                        }
                    }
                    return@a true
                }

                val action = insertActionSync(
                    RedditAction(
                        id = 0,
                        ts = System.currentTimeMillis(),
                        creationTs = System.currentTimeMillis(),
                        info = ActionInfo.VoteActionInfo(id, dir, 2)
                    )
                )

                createLiveDataAndRegisterCallback(action, lifecycleOwner, cb)

                emitter.onSuccess(action)
            }
            .subscribeOn(scheduler)
            .subscribeWithDefaultErrorHandler {}
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
        cb: (DataWithState<RedditAction>) -> Unit
    ) {
        Single
            .create<RedditAction> { emitter ->
                val action = insertActionSync(
                    RedditAction(
                        id = 0,
                        ts = System.currentTimeMillis(),
                        creationTs = System.currentTimeMillis(),
                        info = ActionInfo.CommentActionInfo(parentId = parentId, text = text)
                    )
                )

                createLiveDataAndRegisterCallback(action, lifecycleOwner, cb)

                emitter.onSuccess(action)
            }
            .subscribeOn(scheduler)
            .subscribeWithDefaultErrorHandler {}
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
        cb: (DataWithState<RedditAction>) -> Unit
    ) {
        Single
            .create<RedditAction> { emitter ->
                val action = insertActionSync(
                    RedditAction(
                        id = 0,
                        ts = System.currentTimeMillis(),
                        creationTs = System.currentTimeMillis(),
                        info = ActionInfo.EditActionInfo(thingId = thingId, text = text)
                    )
                )

                createLiveDataAndRegisterCallback(action, lifecycleOwner, cb)

                emitter.onSuccess(action)
            }
            .subscribeOn(scheduler)
            .subscribeWithDefaultErrorHandler {}
    }

    fun deleteComment(
        commentId: String,
        lifecycleOwner: LifecycleOwner,
        cb: (DataWithState<RedditAction>) -> Unit
    ) {
        Single
            .create<RedditAction> { emitter ->
                val action = insertActionSync(
                    RedditAction(
                        id = 0,
                        ts = System.currentTimeMillis(),
                        creationTs = System.currentTimeMillis(),
                        info = ActionInfo.DeleteCommentActionInfo(id = commentId)
                    )
                )

                createLiveDataAndRegisterCallback(action, lifecycleOwner, cb)

                emitter.onSuccess(action)
            }
            .subscribeOn(scheduler)
            .subscribeWithDefaultErrorHandler {}
    }

    /**
     * Creates a live data object for the given [RedditAction] and adds it to the dictionary. Then
     * registers the given call back as a listener to the live data.
     */
    private fun createLiveDataAndRegisterCallback(
        action: RedditAction,
        lifecycleOwner: LifecycleOwner,
        cb: (DataWithState<RedditAction>) -> Unit
    ) {
        val countDownLatch = CountDownLatch(1)

        AndroidSchedulers.mainThread().scheduleDirect {
            val liveData = StatefulLiveData<RedditAction>()
            liveData.observe(lifecycleOwner, Observer {
                if (it != null) {
                    cb(it)
                }
            })
            actionIdToLiveData.put(action.id, liveData as StatefulLiveData<RedditAction>)

            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    fun addActionCompleteListener(l: OnActionChangedListener) {
        onActionChangedListeners.add(l)
    }

    fun removeActionCompleteListener(l: OnActionChangedListener) {
        onActionChangedListeners.remove(l)
    }

    private fun notifyActionComplete(action: RedditAction) {
        for (onActionCompleteListener in onActionChangedListeners) {
            onActionCompleteListener.onActionComplete(action)
        }
    }

    private fun notifyActionAdded(action: RedditAction) {
        for (onActionCompleteListener in onActionChangedListeners) {
            onActionCompleteListener.onActionAdded(action)
        }
    }

    private fun notifyActionFailed(action: RedditAction) {
        for (onActionCompleteListener in onActionChangedListeners) {
            onActionCompleteListener.onActionFailed(action)
        }
    }

    private fun executePendingActions() {
        isExecutingPendingActions = true

        @Suppress("CheckResult")
        Completable
            .fromRunnable a@{
                synchronized(this@PendingActionsManager) {
                    isExecutingPendingActions = actions.isNotEmpty()
                }
                if (!isExecutingPendingActions) {
                    return@a
                }

                val action = actions.removeFirst()

                var connectionIssue = false

                fun completeActionError(action: RedditAction, error: Throwable) {
                    actionIdToLiveData[action.id].postError(PendingActionsException(action, error))
                    actionDao.delete(action).blockingAwait()
                    notifyActionFailed(action)
                }

                fun completeActionSuccess(action: RedditAction) {
                    actionIdToLiveData[action.id].postValue(action)
                    actionDao.delete(action).blockingAwait()
                    notifyActionComplete(action)
                }

                if (action.info.isAffectedByRateLimit && RateLimitManager.isRateLimitHit()) {
                    Log.d(TAG, "Delaying pending action $action")
                    val nextRefresh = RateLimitManager.getTimeUntilNextRefreshMs()
                    delayAction(action, nextRefresh)
                } else {
                    try {
                        Log.d(TAG, "Executing action $action")
                        val result = executeActionSync(action, action.info.retries)

                        Do exhaustive when (result) {
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

                                    action.info.retries++
                                    actions.add(action)
                                } else {
                                    Log.e(
                                        TAG,
                                        "Request failed. Too many retries. ",
                                        RuntimeException()
                                    )

                                    completeActionError(action, RuntimeException())
                                }
                            }
                            is ActionExecutionResult.UnknownError -> {
                                completeActionError(action, RuntimeException())
                            }
                            ActionExecutionResult.ConnectionError -> {
                                connectionIssue = true
                            }
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
            .subscribeOn(scheduler)
            .subscribe()
    }

    private fun delayAction(action: RedditAction, nextRefreshMs: Long) {
        val d = Completable.timer(nextRefreshMs, TimeUnit.MILLISECONDS)
            .observeOn(scheduler)
            .subscribe {
                insertActionSync(action.copy(ts = System.currentTimeMillis()))
            }
    }

    private fun executeActionSync(action: RedditAction, retries: Int): ActionExecutionResult {
        var shouldDeleteAction = true

        actionIdToLiveData[action.id].postIsLoading()

        fun getResultForError(request: Response): ActionExecutionResult {
            val returnCode = request.code

            return if (returnCode == 429) {
                if (RateLimitManager.isRateLimitHit()) {
                    shouldDeleteAction = false

                    Log.d(TAG, "429. Hard limit hit. Rescheduling action...")

                    ActionExecutionResult.RateLimit(RateLimitManager.getTimeUntilNextRefreshMs())
                } else {
                    ActionExecutionResult.TooManyRequests(retries + 1)
                }
            } else {
                actionIdToLiveData[action.id].postError(
                    PendingActionsException(
                        action,
                        ReturnCodeException(returnCode)
                    )
                )

                ActionExecutionResult.UnknownError(returnCode)
            }
        }

        return try {
            when (action.info) {
                is ActionInfo.VoteActionInfo -> {
                    val request = RedditAuthManager.instance.makeAuthedPost(
                        LinkUtils.apiVote(),
                        FormBody.Builder()
                            .add("dir", action.info.dir.toString())
                            .add("id", action.info.id)
                            .build()
                    )

                    val bodyString = request.body?.string()
                    Log.d(TAG, "Result! Body: $bodyString")

                    if (request.isSuccessful) {
                        Log.d(TAG, "Success! Body: $bodyString")

                        actionIdToLiveData[action.id].postValue(action)
                        ActionExecutionResult.Success
                    } else {
                        getResultForError(request)
                    }
                }
                is ActionInfo.CommentActionInfo -> {
                    val request = RedditAuthManager.instance.makeAuthedPost(
                        LinkUtils.apiComment(),
                        FormBody.Builder()
                            .add("thing_id", action.info.parentId)
                            .add("text", action.info.text)
                            .add("api_type", "json")
                            .add("return_rtjson", "true")
                            .build()
                    )

                    val bodyString = request.body?.string()
                    Log.d(TAG, "Result! Body: $bodyString")

                    if (request.isSuccessful) {
                        Log.d(TAG, "Success! Body: $bodyString")

                        val item = Utils.gson.fromJson<RedditCommentItem>(
                            bodyString,
                            RedditCommentItem::class.java
                        )

                        PendingCommentsManager.instance.addPendingComment(item)

                        actionIdToLiveData[action.id].postValue(action)
                        ActionExecutionResult.Success
                    } else {
                        getResultForError(request)
                    }
                }
                is ActionInfo.DeleteCommentActionInfo -> {
                    val request = RedditAuthManager.instance.makeAuthedPost(
                        LinkUtils.apiDeleteComment(),
                        FormBody.Builder()
                            .add("id", action.info.id)
                            .build()
                    )

                    val bodyString = request.body?.string()
                    Log.d(TAG, "Result! Body: $bodyString")

                    if (request.isSuccessful) {
                        Log.d(TAG, "Success! Body: $bodyString")

                        actionIdToLiveData[action.id].postValue(action)
                        ActionExecutionResult.Success
                    } else {
                        getResultForError(request)
                    }
                }
                is ActionInfo.EditActionInfo -> {
                    val request = RedditAuthManager.instance.makeAuthedPost(
                        LinkUtils.apiEditUserText(),
                        FormBody.Builder()
                            .add("api_type", "json")
                            .add("return_rtjson", "true")
                            .add("thing_id", action.info.thingId)
                            .add("text", action.info.text)
                            .build()
                    )

                    val bodyString = request.body?.string()
                    Log.d(TAG, "Result! Body: $bodyString")

                    if (request.isSuccessful) {
                        Log.d(TAG, "Success! Body: $bodyString")

                        val item = Utils.gson.fromJson<RedditCommentItem>(
                            bodyString,
                            RedditCommentItem::class.java
                        )

                        PendingEditsManager.instance.addPendingCommentEdit(item)

                        actionIdToLiveData[action.id].postValue(action)
                        ActionExecutionResult.Success
                    } else {
                        getResultForError(request)
                    }
                }
                else -> throw RuntimeException("Unknown action type ${action.info}")
            }
        } catch (e: UnknownHostException) {
            ActionExecutionResult.ConnectionError
        }
    }

    private fun <T> Single<T>.subscribeWithDefaultErrorHandler(cb: (T) -> Unit): Disposable =
        subscribe(Consumer<T> { cb(it) }, defaultErrorHandler)

    private fun insertActionSync(action: RedditAction): RedditAction {
        val actionId = actionDao.insertAction(action).blockingGet()
        val newAction = action.copy(id = actionId)
        actions.add(newAction)

        if (action.id == 0L) {
            // this is a new item!
            notifyActionAdded(action)
        }

        executePendingActionsIfNeeded()

        return newAction
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
            val retries: Int
        ) : ActionExecutionResult()

        data class UnknownError(
            val errorCode: Int
        ) : ActionExecutionResult()

        object ConnectionError : ActionExecutionResult()
    }

    class ActionRemovedException() :
        Exception("Pending action was removed due to being overriden by a new conflicting action.")

    class PendingActionsException(
        val redditAction: RedditAction,
        override val cause: Throwable
    ) : Exception(cause)
}