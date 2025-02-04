package com.idunnololz.summit.actions.ui

import android.content.Context
import android.os.Parcelable
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.actions.LemmyCompletedAction
import com.idunnololz.summit.lemmy.actions.LemmyFailedAction
import com.idunnololz.summit.lemmy.actions.LemmyPendingAction
import com.idunnololz.summit.lemmy.utils.VotableRef
import kotlinx.parcelize.Parcelize

@Parcelize
data class Action(
    val id: Long,
    val info: ActionInfo?,
    val ts: Long,
    val creationTs: Long,
    val details: ActionDetails,
): Parcelable

fun ActionInfo.getActionName(context: Context) =
    when (this) {
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

fun Action.toLemmyAction(): LemmyAction =
    when (this.details) {
        is ActionDetails.FailureDetails -> {
            LemmyFailedAction(
                id = this.id,
                ts = this.ts,
                creationTs = this.creationTs,
                failedTs = 0,
                error = this.details.reason,
                info = this.info,
            )
        }
        ActionDetails.PendingDetails -> {
            LemmyPendingAction(
                id = this.id,
                ts = this.ts,
                creationTs = this.creationTs,
                info = this.info,
            )
        }
        ActionDetails.SuccessDetails -> {
            LemmyCompletedAction(
                id = this.id,
                ts = this.ts,
                creationTs = this.creationTs,
                info = this.info,
            )
        }
    }