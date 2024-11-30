package com.idunnololz.summit.actions

import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingCommentsManager @Inject constructor() {

    private val idToPendingCommentView = mutableMapOf<Long, PendingCommentView>()
    private val pendingCommentsDict = hashMapOf<PostRef, MutableList<PendingCommentView>>()

    fun getPendingComments(postRef: PostRef): List<PendingCommentView> {
        return ArrayList(pendingCommentsDict[postRef] ?: return listOf())
    }

    fun removePendingComment(pendingCommentView: PendingCommentView) {
        pendingCommentsDict.getOrPut(pendingCommentView.postRef) { mutableListOf() }
            .remove(pendingCommentView)
        idToPendingCommentView.remove(pendingCommentView.id)
    }

    fun onCommentActionFailed(
        id: Long,
        info: ActionInfo.CommentActionInfo,
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
            )
        addPendingComment(pendingCommentView)
    }

    fun onCommentActionComplete(id: Long, info: ActionInfo.CommentActionInfo) {
        idToPendingCommentView[id]?.complete = true
    }

    fun onEditCommentActionAdded(id: Long, action: ActionInfo.EditActionInfo) {
        val pendingCommentView =
            PendingCommentView(
                actionId = id,
                postRef = action.postRef,
                commentId = action.commentId,
                parentId = null,
                content = action.content,
                accountId = action.accountId,
            )
        addPendingComment(pendingCommentView)
    }

    fun onEditCommentActionFailed(
        id: Long,
        info: ActionInfo.EditActionInfo,
        reason: LemmyActionFailureReason,
    ) {
        idToPendingCommentView[id]?.error = reason
    }

    fun onEditCommentActionComplete(id: Long, info: ActionInfo.EditActionInfo) {
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
    val accountId: Long,
    val isActionDelete: Boolean = false,
    var error: LemmyActionFailureReason? = null,
    var complete: Boolean = false,
) {
    val id: Long
        get() = actionId
}
