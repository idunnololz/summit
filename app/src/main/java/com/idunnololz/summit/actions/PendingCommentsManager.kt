package com.idunnololz.summit.actions

import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.api.dto.lemmy.LanguageId
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import com.idunnololz.summit.lemmy.actions.LemmyActionsDao
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IMPORTANT: Pending comments are any comment actions that are in the pending or failed state.
 */
@Singleton
class PendingCommentsManager @Inject constructor(
  coroutineScopeFactory: CoroutineScopeFactory,
  private val actionsDao: LemmyActionsDao,
) {

  private val coroutineScope = coroutineScopeFactory.create()

  private val idToPendingCommentView = mutableMapOf<Long, PendingCommentView>()
  private val pendingCommentsDict = hashMapOf<PostRef, MutableList<PendingCommentView>>()

  private var initialized = false
  private var mutex = Mutex()

  private suspend fun initIfNeeded() {
    mutex.withLock {
      if (initialized) {
        return
      }

      initialized = true

      val failedOrPendingActions = actionsDao.getLast100FailedActions() +
        actionsDao.getAllPendingActions()
      failedOrPendingActions.forEach {
        if (it.info is ActionInfo.CommentActionInfo) {
          onCommentActionAdded(id = it.id, action = it.info)

          if (it.error != null) {
            onCommentActionFailed(id = it.id, reason = it.error)
          }
        }
      }
    }
  }

  suspend fun getPendingComments(postRef: PostRef): List<PendingCommentView> {
    initIfNeeded()
    return ArrayList(pendingCommentsDict[postRef] ?: return listOf())
  }

  fun removePendingComment(pendingCommentView: PendingCommentView) {
    pendingCommentsDict.getOrPut(pendingCommentView.postRef) { mutableListOf() }
      .remove(pendingCommentView)
    idToPendingCommentView.remove(pendingCommentView.id)
  }

  fun onCommentActionFailed(
    id: Long,
    reason: LemmyActionFailureReason,
  ) {
    idToPendingCommentView[id]?.error = reason
  }

  fun onCommentActionAdded(id: Long, action: ActionInfo.CommentActionInfo) {
    val pendingCommentView =
      PendingCommentView(
        actionId = id,
        postRef = action.postRef,
        commentId = null,
        parentId = action.parentId,
        content = action.content,
        accountId = action.accountId,
        languageId = action.languageId,
      )
    addPendingComment(pendingCommentView)
  }

  fun onCommentActionComplete(id: Long, info: ActionInfo.CommentActionInfo) {
    idToPendingCommentView[id]?.complete = true
  }

  fun onCommentActionDeleted(id: Long) {
    val pendingComment = idToPendingCommentView[id] ?: return
    removePendingComment(pendingComment)
  }

  fun onEditCommentActionAdded(id: Long, action: ActionInfo.EditCommentActionInfo) {
    val pendingCommentView =
      PendingCommentView(
        actionId = id,
        postRef = action.postRef,
        commentId = action.commentId,
        parentId = null,
        content = action.content,
        accountId = action.accountId,
        languageId = action.languageId,
      )
    addPendingComment(pendingCommentView)
  }

  fun onEditCommentActionFailed(
    id: Long,
    info: ActionInfo.EditCommentActionInfo,
    reason: LemmyActionFailureReason,
  ) {
    idToPendingCommentView[id]?.error = reason
  }

  fun onEditCommentActionComplete(id: Long, info: ActionInfo.EditCommentActionInfo) {
    idToPendingCommentView[id]?.complete = true
  }

  fun onDeleteCommentActionAdded(id: Long, action: ActionInfo.DeleteCommentActionInfo) {
    val pendingCommentView =
      PendingCommentView(
        actionId = id,
        postRef = action.postRef,
        commentId = action.commentId,
        parentId = null,
        content = "",
        languageId = null,
        accountId = action.accountId,
        isActionDelete = true,
      )
    addPendingComment(pendingCommentView)
  }

  fun onDeleteCommentActionFailed(
    id: Long,
    info: ActionInfo.DeleteCommentActionInfo,
    reason: LemmyActionFailureReason,
  ) {
    idToPendingCommentView[id]?.error = reason
  }

  fun onDeleteCommentActionComplete(id: Long, info: ActionInfo.DeleteCommentActionInfo) {
    idToPendingCommentView[id]?.complete = true
  }

  private fun addPendingComment(pendingCommentView: PendingCommentView) {
    idToPendingCommentView[pendingCommentView.id] = pendingCommentView
    pendingCommentsDict.getOrPut(pendingCommentView.postRef) { mutableListOf() }
      .add(pendingCommentView)
  }
}

data class PendingCommentView(
  val actionId: Long,
  val postRef: PostRef,
  val commentId: CommentId?,
  val parentId: CommentId?,
  val content: String,
  val languageId: LanguageId?,
  val accountId: Long,
  val isActionDelete: Boolean = false,
  var error: LemmyActionFailureReason? = null,
  var complete: Boolean = false,
) {
  val id: Long
    get() = actionId
}
