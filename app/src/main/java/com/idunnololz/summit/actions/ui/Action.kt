package com.idunnololz.summit.actions.ui

import android.content.Context
import android.os.Parcelable
import androidx.room.ColumnInfo
import com.idunnololz.summit.R
import com.idunnololz.summit.actions.ui.ActionDetails.*
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.ActionStatus
import com.idunnololz.summit.lemmy.actions.OldLemmyCompletedAction
import com.idunnololz.summit.lemmy.actions.OldLemmyFailedAction
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import kotlinx.parcelize.Parcelize

@Parcelize
data class Action(
  val id: Long,
  val info: ActionInfo?,
  val ts: Long,
  val creationTs: Long,
  val details: ActionDetails,
  val seen: Boolean,
  val failedTs: Long? = null,
  val error: LemmyActionFailureReason? = null,
  val completedTs: Long? = null,
) : Parcelable

fun LemmyAction.toAction() =
  Action(
    id = this.id,
    info = this.info,
    ts = this.ts,
    creationTs = this.creationTs,
    details =
      when (this.status) {
        null,
        ActionStatus.Pending -> ActionDetails.PendingDetails
        ActionStatus.Errored -> FailureDetails(
          this.error,
        )
        ActionStatus.Completed -> ActionDetails.SuccessDetails
      },
    seen = this.seen ?: false,
    failedTs = this.failedTs,
    error = this.error,
    completedTs = this.completedTs,
  )

fun ActionInfo.getActionName(context: Context) = when (this) {
  is ActionInfo.CommentActionInfo ->
    context.getString(R.string.comment)
  is ActionInfo.DeleteCommentActionInfo ->
    context.getString(R.string.delete_comment)
  is ActionInfo.EditCommentActionInfo ->
    context.getString(R.string.edit_comment)
  is ActionInfo.MarkPostAsReadActionInfo ->
    if (read) {
      context.getString(R.string.mark_post_as_read)
    } else {
      context.getString(R.string.mark_post_as_unread)
    }
  is ActionInfo.VoteActionInfo ->
    if (dir == 0) {
      context.getString(R.string.clear_vote)
    } else if (dir > 0) {
      context.getString(R.string.upvote)
    } else {
      context.getString(R.string.downvote)
    }
}

fun Action.toLemmyAction(): LemmyAction = when (this.details) {
  is FailureDetails -> {
    LemmyAction(
      id = this.id,
      ts = this.ts,
      creationTs = this.creationTs,
      info = this.info,
      status = ActionStatus.Errored,
      seen = this.seen,
      failedTs = this.failedTs,
      error = this.details.reason,
      completedTs = this.completedTs,
    )
  }
  PendingDetails -> {
    LemmyAction(
      id = this.id,
      ts = this.ts,
      creationTs = this.creationTs,
      info = this.info,
      seen = this.seen,
      failedTs = this.failedTs,
      completedTs = this.completedTs,
    )
  }
  SuccessDetails -> {
    LemmyAction(
      id = this.id,
      ts = this.ts,
      creationTs = this.creationTs,
      info = this.info,
      status = ActionStatus.Completed,
      seen = this.seen,
      failedTs = this.failedTs,
      completedTs = this.completedTs,
    )
  }
}
