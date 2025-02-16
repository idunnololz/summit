package com.idunnololz.summit.actions

import android.content.Context
import android.util.Log
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.actions.LemmyPendingAction
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import com.idunnololz.summit.lemmy.actions.LemmyActionResult
import com.idunnololz.summit.lemmy.actions.LemmyActionsDao
import com.idunnololz.summit.lemmy.actions.LemmyCompletedAction
import com.idunnololz.summit.lemmy.actions.LemmyCompletedActionsDao
import com.idunnololz.summit.lemmy.actions.LemmyFailedAction
import com.idunnololz.summit.lemmy.actions.LemmyFailedActionsDao
import com.idunnololz.summit.lemmy.utils.VotableRef
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

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
        fun onActionAdded(action: LemmyPendingAction)
        fun onActionFailed(action: LemmyPendingAction, reason: LemmyActionFailureReason)
        fun onActionComplete(action: LemmyPendingAction, result: LemmyActionResult<*, *>)
        fun onActionDeleted(action: LemmyAction)
    }

    /**
     * Modifications to [actions] must be made with the [actionsContext].
     */
    private val actions = LinkedList<LemmyPendingAction>()

    private val failedActions = LinkedList<LemmyFailedAction>()

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val actionsContext = newSingleThreadContext("CounterContext")

    private val onActionChangedListeners = arrayListOf<OnActionChangedListener>()

    private val coroutineScope = coroutineScopeFactory.create()

    private val pendingActionsRunner = pendingActionsRunnerFactory.create(
        actions = actions,
        coroutineScope = coroutineScope,
        actionsContext = actionsContext,
        delayAction = { action: LemmyPendingAction, nextRefreshMs: Long ->
            delayAction(action, nextRefreshMs)
        },
        completeActionError = { action: LemmyPendingAction, failureReason: LemmyActionFailureReason ->
            completeActionError(action, failureReason)
        },
        completeActionSuccess = { action: LemmyPendingAction, result: LemmyActionResult<*, *> ->
            completeActionSuccess(action, result)
        },
    )

    val numNewFailedActionsFlow = MutableStateFlow<Int>(0)

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
                updateNewErrorsCount()

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

    fun getAllPendingActions(): List<LemmyPendingAction> = actions

    fun markActionAsSeen(action: LemmyAction) {
        coroutineScope.launch {
            withContext(actionsContext) {
                when (action) {
                    is LemmyCompletedAction -> {}
                    is LemmyFailedAction -> {
                        val updatedAction = action.copy(
                            seen = true
                        )
                        failedActionsDao.insertFailedAction(updatedAction)
                        val pos = failedActions.indexOfFirst { it.id == action.id }
                        if (pos >= 0) {
                            failedActions[pos] = updatedAction
                            updateNewErrorsCount()
                        }
                    }

                    is LemmyPendingAction -> {}
                }
            }
        }
    }

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
        accountId: Long,
        accountInstance: String,
    ) {
        coroutineScope.launch {
            val action = ActionInfo.VoteActionInfo(
                instance = instance,
                ref = ref,
                dir = dir,
                rank = 2,
                accountId = accountId,
                accountInstance = accountInstance,
            )
            action.removeSimilarActionsBy { ref }
            action.insert()
        }
    }

    /**
     * Posts a comment/reply to another comment.
     * @param parentId what to comment on
     * @param content text to post
     */
    suspend fun comment(
        postRef: PostRef,
        parentId: CommentId?,
        content: String,
        accountId: Long,
        accountInstance: String,
    ): LemmyPendingAction {
        val lemmyAction = coroutineScope.async {
            val action = ActionInfo.CommentActionInfo(
                postRef = postRef,
                parentId = parentId,
                content = content,
                accountId = accountId,
                accountInstance = accountInstance,
            )
            action.insert()
        }

        return lemmyAction.await()
    }

    /**
     * Edit the body text of a comment or self-post.
     * @param commentId what to edit
     * @param content text to post
     */
    suspend fun editComment(
        postRef: PostRef,
        commentId: CommentId,
        content: String,
        accountId: Long,
        accountInstance: String,
    ): LemmyPendingAction {
        val lemmyAction = coroutineScope.async {
            val action = ActionInfo.EditCommentActionInfo(
                postRef = postRef,
                commentId = commentId,
                content = content,
                accountId = accountId,
                accountInstance = accountInstance,
            )
            action.insert()
        }

        return lemmyAction.await()
    }

    suspend fun deleteComment(
        postRef: PostRef,
        commentId: CommentId,
        accountId: Long,
        accountInstance: String,
    ): LemmyPendingAction {
        val lemmyAction = coroutineScope.async {
            val action = ActionInfo.DeleteCommentActionInfo(
                postRef = postRef,
                commentId = commentId,
                accountId = accountId,
                accountInstance = accountInstance,
            )
            action.insert()
        }

        return lemmyAction.await()
    }

    suspend fun markPostAsRead(
        postRef: PostRef,
        read: Boolean,
        accountId: Long,
        accountInstance: String,
    ): LemmyPendingAction {
        val lemmyAction = coroutineScope.async {
            val action = ActionInfo.MarkPostAsReadActionInfo(
                postRef = postRef,
                read = read,
                accountId = accountId,
                accountInstance = accountInstance,
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

    private suspend fun notifyActionComplete(action: LemmyPendingAction, result: LemmyActionResult<*, *>) =
        withContext(
            Dispatchers.Main,
        ) {
            for (onActionCompleteListener in onActionChangedListeners) {
                onActionCompleteListener.onActionComplete(action, result)
            }
        }

    private suspend fun notifyActionAdded(action: LemmyPendingAction) = withContext(Dispatchers.Main) {
        for (onActionCompleteListener in onActionChangedListeners) {
            onActionCompleteListener.onActionAdded(action)
        }
    }

    private suspend fun notifyActionFailed(
        action: LemmyPendingAction,
        reason: LemmyActionFailureReason,
    ) =
        withContext(Dispatchers.Main) {
            for (onActionCompleteListener in onActionChangedListeners) {
                onActionCompleteListener.onActionFailed(action, reason)
            }
        }

    private suspend fun notifyActionDeleted(action: LemmyAction) =
        withContext(Dispatchers.Main) {
            for (onActionCompleteListener in onActionChangedListeners) {
                onActionCompleteListener.onActionDeleted(action)
            }
        }

    private suspend fun insertAction(action: LemmyPendingAction): LemmyPendingAction =
        withContext(Dispatchers.Default) {
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
    ) = withContext(actionsContext) {
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

    private suspend fun ActionInfo.insert(): LemmyPendingAction = insertAction(
        LemmyPendingAction(
            id = 0,
            ts = System.currentTimeMillis(),
            creationTs = System.currentTimeMillis(),
            info = this,
        ),
    )

    private fun delayAction(action: LemmyPendingAction, nextRefreshMs: Long) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                delay(nextRefreshMs)
            }

            withContext(actionsContext) {
                insertAction(action.copy(ts = System.currentTimeMillis()))
            }
        }
    }

    suspend fun completeActionError(action: LemmyPendingAction, error: LemmyActionFailureReason) {
        withContext(actionsContext) {
            actionsDao.delete(action)

            action.toLemmyFailedAction(error).let {
                val id = failedActionsDao.insertFailedAction(it)
                failedActions.add(it.copy(id = id))
            }
            updateNewErrorsCount()
        }
        notifyActionFailed(action, error)
    }

    suspend fun completeActionSuccess(
        action: LemmyPendingAction,
        result: LemmyActionResult<*, *>,
    ) {
        withContext(actionsContext) {
            actionsDao.delete(action)

            completedActionsDao.insertActionRespectingTableLimit(action.toLemmyCompletedAction())
        }
        notifyActionComplete(action, result)
    }

    suspend fun deleteFailedAction(
        action: LemmyFailedAction
    ) {
        withContext(actionsContext) {
            failedActionsDao.delete(action)

            val it = failedActions.iterator()
            while (it.hasNext()) {
                val next = it.next()

                if (next.id == action.id) {
                    it.remove()
                }
            }
            updateNewErrorsCount()
        }
        notifyActionDeleted(action)
    }

    suspend fun deleteCompletedAction(
        action: LemmyCompletedAction
    ) {
        withContext(actionsContext) {
            completedActionsDao.delete(action)
        }
        notifyActionDeleted(action)
    }

    suspend fun deletePendingAction(
        action: LemmyPendingAction
    ) {
        withContext(actionsContext) {
            actionsDao.delete(action)
        }
        notifyActionDeleted(action)
    }

    sealed class ActionExecutionResult {
        class Success(val result: LemmyActionResult<*, *>) : ActionExecutionResult()
        data class Failure(
            val failureReason: LemmyActionFailureReason,
        ) : ActionExecutionResult()
    }

    private fun LemmyPendingAction.toLemmyFailedAction(
        error: LemmyActionFailureReason,
    ): LemmyFailedAction = LemmyFailedAction(
        id = 0L,
        ts = this.ts,
        creationTs = this.creationTs,
        failedTs = System.currentTimeMillis(),
        error = error,
        info = this.info,
    )

    private fun LemmyPendingAction.toLemmyCompletedAction() = LemmyCompletedAction(
        id = 0L,
        ts = System.currentTimeMillis(),
        creationTs = creationTs,
        info = info,
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

    private fun updateNewErrorsCount() {
        numNewFailedActionsFlow.value = failedActions.count { it.seen != true }
    }
}
