package com.idunnololz.summit.actions

import android.content.Context
import android.util.Log
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import com.idunnololz.summit.lemmy.actions.LemmyActionResult
import com.idunnololz.summit.lemmy.actions.LemmyActionsDao
import com.idunnololz.summit.lemmy.actions.LemmyCompletedAction
import com.idunnololz.summit.lemmy.actions.LemmyCompletedActionsDao
import com.idunnololz.summit.lemmy.actions.LemmyFailedAction
import com.idunnololz.summit.lemmy.actions.LemmyFailedActionsDao
import com.idunnololz.summit.lemmy.utils.VotableRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingActionsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    coroutineScopeFactory: CoroutineScopeFactory,
    private val actionsDao: LemmyActionsDao,
    private val failedActionsDao: LemmyFailedActionsDao,
    private val completedActionsDao: LemmyCompletedActionsDao,
    pendingActionsRunnerFactory: PendingActionsRunner.Factory,
) {

    companion object {

        private const val TAG = "PendingActionsManager"

        const val MAX_RETRIES = 3
    }

    interface OnActionChangedListener {
        fun onActionAdded(action: LemmyAction)
        fun onActionFailed(action: LemmyAction, reason: LemmyActionFailureReason)
        fun onActionComplete(action: LemmyAction, result: LemmyActionResult<*, *>)
    }

    /**
     * Modifications to [actions] must be made with the [actionsContext].
     */
    private val actions = LinkedList<LemmyAction>()

    private val failedActions = LinkedList<LemmyFailedAction>()

    @OptIn(DelicateCoroutinesApi::class)
    private val actionsContext = newSingleThreadContext("CounterContext")

    private val onActionChangedListeners = arrayListOf<OnActionChangedListener>()

    private val coroutineScope = coroutineScopeFactory.create()

    private val pendingActionsRunner = pendingActionsRunnerFactory.create(
        actions = actions,
        coroutineScope = coroutineScope,
        actionsContext = actionsContext,
        delayAction = { action: LemmyAction, nextRefreshMs: Long ->
            delayAction(action, nextRefreshMs)
        },
        completeActionError = { action: LemmyAction, failureReason: LemmyActionFailureReason ->
            completeActionError(action, failureReason)
        },
        completeActionSuccess = { action: LemmyAction, result: LemmyActionResult<*, *> ->
            completeActionSuccess(action, result)
        },
    )

    init {
        coroutineScope.launch {
            val dbActions = actionsDao.getAllPendingActions()
            val dbFailedActions = failedActionsDao.getLast100FailedActions()

            Log.d("dbdb", "actionsDao: ${actionsDao.count()}")
            Log.d("dbdb", "failedActionsDao: ${failedActionsDao.count()}")

            withContext(actionsContext) {
                actions.clear()
                actions.addAll(dbActions)
                actions.sortedBy { it.ts }

                failedActions.clear()
                failedActions.addAll(dbFailedActions)

                for (action in actions) {
                    Log.d(TAG, "Restored action with id ${action.id}")
                    notifyActionAdded(action)
                }
            }

            executePendingActionsIfNeeded()
        }
    }

    fun executePendingActionsIfNeeded() {
        Log.d(TAG, "executePendingActionsIfNeeded()")
        pendingActionsRunner.executePendingActionsIfNeeded()
    }

    fun getAllPendingActions(): List<LemmyAction> = actions

    suspend fun getAllCompletedActions(): List<LemmyCompletedAction> =
        completedActionsDao.getAllCompletedActions()

    suspend fun getAllFailedActions(): List<LemmyFailedAction> =
        failedActionsDao.getAllFailedActions()

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
            val action = ActionInfo.VoteActionInfo(
                instance = instance,
                ref = ref,
                dir = dir,
                rank = 2,
                accountId = accountId,
            )
            action.removeSimilarActionsBy { ref }
            action.insert()
        }
    }

    /**
     * Posts a comment/reply to another comment.
     * @param parentId what to comment on
     * @param text text to post
     */
    suspend fun comment(
        postRef: PostRef,
        parentId: CommentId?,
        content: String,
        accountId: Int,
    ): LemmyAction {
        val lemmyAction = coroutineScope.async {
            val action = ActionInfo.CommentActionInfo(
                postRef = postRef,
                parentId = parentId,
                content = content,
                accountId = accountId,
            )
            action.insert()
        }

        return lemmyAction.await()
    }

    /**
     * Edit the body text of a comment or self-post.
     * @param thingId what to edit
     * @param text text to post
     */
    suspend fun editComment(
        postRef: PostRef,
        commentId: CommentId,
        content: String,
        accountId: Int,
    ): LemmyAction {
        val lemmyAction = coroutineScope.async {
            val action = ActionInfo.EditActionInfo(
                postRef = postRef,
                commentId = commentId,
                content = content,
                accountId = accountId,
            )
            action.insert()
        }

        return lemmyAction.await()
    }

    suspend fun deleteComment(
        postRef: PostRef,
        commentId: CommentId,
        accountId: Int,
    ): LemmyAction {
        val lemmyAction = coroutineScope.async {
            val action = ActionInfo.DeleteCommentActionInfo(
                postRef = postRef,
                commentId = commentId,
                accountId = accountId,
            )
            action.insert()
        }

        return lemmyAction.await()
    }

    suspend fun markPostAsRead(
        postRef: PostRef,
        read: Boolean,
        accountId: Int,
    ): LemmyAction {
        val lemmyAction = coroutineScope.async {
            val action = ActionInfo.MarkPostAsReadActionInfo(
                postRef = postRef,
                read = read,
                accountId = accountId,
            )
            action.insert()
        }

        return lemmyAction.await()
    }

    fun addActionCompleteListener(l: OnActionChangedListener) {
        onActionChangedListeners.add(l)
    }

    fun removeActionCompleteListener(l: OnActionChangedListener) {
        onActionChangedListeners.remove(l)
    }

    private suspend fun notifyActionComplete(action: LemmyAction, result: LemmyActionResult<*, *>) = withContext(Dispatchers.Main) {
        for (onActionCompleteListener in onActionChangedListeners) {
            onActionCompleteListener.onActionComplete(action, result)
        }
    }

    private suspend fun notifyActionAdded(action: LemmyAction) = withContext(Dispatchers.Main) {
        for (onActionCompleteListener in onActionChangedListeners) {
            onActionCompleteListener.onActionAdded(action)
        }
    }

    private suspend fun notifyActionFailed(
        action: LemmyAction,
        reason: LemmyActionFailureReason,
    ) = withContext(Dispatchers.Main) {
        for (onActionCompleteListener in onActionChangedListeners) {
            onActionCompleteListener.onActionFailed(action, reason)
        }
    }

    private suspend fun insertAction(
        action: LemmyAction,
    ): LemmyAction = withContext(Dispatchers.Default) {
        val newAction = withContext(actionsContext) {
            val actionId = actionsDao.insertAction(action)
            val newAction = action.copy(id = actionId)

            actions.add(newAction)

            newAction
        }

        if (action.id == 0L) {
            // this is a new item!
            notifyActionAdded(newAction)
        }

        executePendingActionsIfNeeded()

        newAction
    }

    private suspend inline fun <reified T : ActionInfo, ID> T.removeSimilarActionsBy(
        crossinline id: T.() -> ID,
    ) =
        withContext(actionsContext) {
            val iterator = actions.iterator()
            while (iterator.hasNext()) {
                val action = iterator.next()

                if (action.info is T) {
                    if (action.info.id() == this@removeSimilarActionsBy) {
                        iterator.remove()
                        actionsDao.delete(action)

                        notifyActionFailed(action, LemmyActionFailureReason.ActionOverwritten)
                    }
                }
            }
        }

    private suspend fun ActionInfo.insert(): LemmyAction =
        insertAction(
            LemmyAction(
                id = 0,
                ts = System.currentTimeMillis(),
                creationTs = System.currentTimeMillis(),
                info = this,
            ),
        )

    private fun delayAction(action: LemmyAction, nextRefreshMs: Long) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                delay(nextRefreshMs)
            }

            withContext(actionsContext) {
                insertAction(action.copy(ts = System.currentTimeMillis()))
            }
        }
    }

    private suspend fun completeActionError(action: LemmyAction, error: LemmyActionFailureReason) {
        withContext(actionsContext) {
            actionsDao.delete(action)

            action.toLemmyFailedAction(error).let {
                failedActionsDao.insertFailedAction(it)
                failedActions.add(it)
            }
        }
        notifyActionFailed(action, error)
    }

    private suspend fun completeActionSuccess(action: LemmyAction, result: LemmyActionResult<*, *>) {
        withContext(actionsContext) {
            actionsDao.delete(action)

            completedActionsDao.insertActionRespectingTableLimit(action.toLemmyCompletedAction())
        }
        notifyActionComplete(action, result)
    }

    sealed class ActionExecutionResult {
        class Success(val result: LemmyActionResult<*, *>) : ActionExecutionResult()
        data class Failure(
            val failureReason: LemmyActionFailureReason,
        ) : ActionExecutionResult()
    }

    private fun LemmyAction.toLemmyFailedAction(
        error: LemmyActionFailureReason,
    ): LemmyFailedAction =
        LemmyFailedAction(
            id = 0L,
            ts = this.ts,
            creationTs = this.creationTs,
            failedTs = System.currentTimeMillis(),
            error = error,
            info = this.info,
        )

    private fun LemmyAction.toLemmyCompletedAction() =
        LemmyCompletedAction(
            id = 0L,
            ts = ts,
            creationTs = creationTs,
            info = info
        )

    suspend fun deleteCompletedActions() {
        completedActionsDao.deleteAllActions()
    }

    suspend fun deleteFailedActions() {
        failedActionsDao.deleteAllFailedActions()
    }

    suspend fun deleteAllPendingActions() {
        withContext(actionsContext) {
            pendingActionsRunner.stopPendingActions()
            actions.clear()
        }

        actionsDao.deleteAllActions()
    }
}
