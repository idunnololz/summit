package com.idunnololz.summit.lemmy.utils

import android.content.res.ColorStateList
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.api.AccountInstanceMismatchException
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.inbox.CommentBackedItem
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

private val TAG = "VoteUiHandler"

interface VoteUiHandler {
    fun bindVoteUi(
        lifecycleOwner: LifecycleOwner,
        currentVote: Int,
        currentScore: Int,
        upVotes: Int,
        downVotes: Int,
        instance: String,
        ref: VotableRef,
        upVoteView: View?,
        downVoteView: View?,
        scoreView: TextView,
        upvoteCount: TextView?,
        downvoteCount: TextView?,
        registration: AccountActionsManager.Registration,
    )
    fun unbindVoteUi(scoreView: View)

    val upvoteColor: Int
    val downvoteColor: Int
}

fun VoteUiHandler.bind(
    lifecycleOwner: LifecycleOwner,
    instance: String,
    commentView: CommentView,
    upVoteView: View?,
    downVoteView: View?,
    scoreView: TextView,
    upvoteCount: TextView?,
    downvoteCount: TextView?,
    onUpdate: ((score: Int) -> Unit)?,
    onSignInRequired: () -> Unit,
    onInstanceMismatch: (String, String) -> Unit,
) {
    bind(
        lifecycleOwner = lifecycleOwner,
        instance = instance,
        currentVote = commentView.my_vote ?: 0,
        currentScore = commentView.counts.score,
        upvotes = commentView.counts.upvotes,
        downvotes = commentView.counts.downvotes,
        ref = VotableRef.CommentRef(commentView.comment.id),
        upVoteView = upVoteView,
        downVoteView = downVoteView,
        scoreView = scoreView,
        upvoteCount = upvoteCount,
        downvoteCount = downvoteCount,
        onUpdate = onUpdate,
        onSignInRequired = onSignInRequired,
        onInstanceMismatch = onInstanceMismatch,
    )
}

fun VoteUiHandler.bind(
    lifecycleOwner: LifecycleOwner,
    instance: String,
    postView: PostView,
    upVoteView: View?,
    downVoteView: View?,
    scoreView: TextView,
    upvoteCount: TextView?,
    downvoteCount: TextView?,
    onUpdate: ((score: Int) -> Unit)?,
    onSignInRequired: () -> Unit,
    onInstanceMismatch: (String, String) -> Unit,
) {
    bind(
        lifecycleOwner = lifecycleOwner,
        instance = instance,
        currentVote = postView.my_vote ?: 0,
        currentScore = postView.counts.score,
        upvotes = postView.counts.upvotes,
        downvotes = postView.counts.downvotes,
        ref = VotableRef.PostRef(postView.post.id),
        upVoteView = upVoteView,
        downVoteView = downVoteView,
        scoreView = scoreView,
        upvoteCount = upvoteCount,
        downvoteCount = downvoteCount,
        onUpdate = onUpdate,
        onSignInRequired = onSignInRequired,
        onInstanceMismatch = onInstanceMismatch,
    )
}

fun VoteUiHandler.bind(
    lifecycleOwner: LifecycleOwner,
    instance: String,
    inboxItem: CommentBackedItem,
    upVoteView: View?,
    downVoteView: View?,
    scoreView: TextView,
    upvoteCount: TextView?,
    downvoteCount: TextView?,
    onUpdate: ((score: Int) -> Unit)?,
    onSignInRequired: () -> Unit,
    onInstanceMismatch: (String, String) -> Unit,
) {
    bind(
        lifecycleOwner = lifecycleOwner,
        instance = instance,
        currentVote = inboxItem.myVote ?: 0,
        currentScore = inboxItem.score,
        upvotes = inboxItem.upvotes,
        downvotes = inboxItem.downvotes,
        ref = VotableRef.CommentRef(inboxItem.commentId),
        upVoteView = upVoteView,
        downVoteView = downVoteView,
        scoreView = scoreView,
        upvoteCount = upvoteCount,
        downvoteCount = downvoteCount,
        onUpdate = onUpdate,
        onSignInRequired = onSignInRequired,
        onInstanceMismatch = onInstanceMismatch,
    )
}

fun VoteUiHandler.bind(
    lifecycleOwner: LifecycleOwner,
    instance: String,
    currentVote: Int,
    currentScore: Int,
    upvotes: Int,
    downvotes: Int,
    ref: VotableRef,
    upVoteView: View?,
    downVoteView: View?,
    scoreView: TextView,
    upvoteCount: TextView?,
    downvoteCount: TextView?,
    onUpdate: ((score: Int) -> Unit)?,
    onSignInRequired: () -> Unit,
    onInstanceMismatch: (String, String) -> Unit,
) {
    val context = scoreView.context
    fun update(score: Int) {
        if (upVoteView is ImageView && downVoteView is ImageView) {
            if (score < 0) {
                upVoteView.setColorFilter(
                    context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
                downVoteView.setColorFilter(downvoteColor)
            } else if (score > 0) {
                upVoteView.setColorFilter(upvoteColor)
                downVoteView.setColorFilter(context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
            } else {
                upVoteView.setColorFilter(context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
                downVoteView.setColorFilter(context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
            }
        } else if (upVoteView is MaterialButton && downVoteView is MaterialButton) {
            if (score < 0) {
                upVoteView.iconTint =
                    ColorStateList.valueOf(
                        context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
                downVoteView.iconTint =
                    ColorStateList.valueOf(downvoteColor)
            } else if (score > 0) {
                upVoteView.iconTint =
                    ColorStateList.valueOf(upvoteColor)
                downVoteView.iconTint =
                    ColorStateList.valueOf(
                        context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
            } else {
                upVoteView.iconTint =
                    ColorStateList.valueOf(
                        context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
                downVoteView.iconTint =
                    ColorStateList.valueOf(
                        context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
            }
        }
        if (score < 0) {
            if (downvoteCount == null || upvoteCount == null) {
                scoreView.setTextColor(downvoteColor)
            } else {
                downvoteCount.setTextColor(downvoteColor)
                upvoteCount.setTextColor(context.getColorCompat(R.color.colorText))
            }
        } else if (score > 0) {
            if (downvoteCount == null || upvoteCount == null) {
                scoreView.setTextColor(upvoteColor)
            } else {
                downvoteCount.setTextColor(context.getColorCompat(R.color.colorText))
                upvoteCount.setTextColor(upvoteColor)
            }
        } else {
            if (downvoteCount == null || upvoteCount == null) {
                scoreView.setTextColor(context.getColorCompat(R.color.colorText))
            } else {
                downvoteCount.setTextColor(context.getColorCompat(R.color.colorText))
                upvoteCount.setTextColor(context.getColorCompat(R.color.colorText))
            }
        }

        upVoteView?.invalidate()
        downVoteView?.invalidate()

        onUpdate?.invoke(score)
    }
    bindVoteUi(
        lifecycleOwner,
        currentVote,
        currentScore,
        upvotes,
        downvotes,
        instance,
        ref,
        upVoteView,
        downVoteView,
        scoreView,
        upvoteCount,
        downvoteCount,
        object : AccountActionsManager.Registration {
            override fun voteCurrent(score: Int, totalScore: Int?, upvotes: Int?, downvotes: Int?,) {
                update(score)
                scoreView.text = LemmyUtils.abbrevNumber(totalScore?.toLong())
                upvoteCount?.text = LemmyUtils.abbrevNumber(upvotes?.toLong())
                downvoteCount?.text = LemmyUtils.abbrevNumber(downvotes?.toLong())
            }

            override fun voteSuccess(newScore: Int, totalScore: Int?, upvotes: Int?, downvotes: Int?,) {
                update(newScore)
                scoreView.text = LemmyUtils.abbrevNumber(totalScore?.toLong())
                upvoteCount?.text = LemmyUtils.abbrevNumber(upvotes?.toLong())
                downvoteCount?.text = LemmyUtils.abbrevNumber(downvotes?.toLong())
            }

            override fun votePending(pendingScore: Int, totalScore: Int?, upvotes: Int?, downvotes: Int?,) {
                update(pendingScore)
                scoreView.text = LemmyUtils.abbrevNumber(totalScore?.toLong())
                upvoteCount?.text = LemmyUtils.abbrevNumber(upvotes?.toLong())
                downvoteCount?.text = LemmyUtils.abbrevNumber(downvotes?.toLong())
            }

            override fun voteFailed(score: Int, totalScore: Int?, upvotes: Int?, downvotes: Int?, e: Throwable) {
                update(score)
                scoreView.text = LemmyUtils.abbrevNumber(totalScore?.toLong())
                upvoteCount?.text = LemmyUtils.abbrevNumber(upvotes?.toLong())
                downvoteCount?.text = LemmyUtils.abbrevNumber(downvotes?.toLong())

                if (e is NotAuthenticatedException) {
                    onSignInRequired()
                    return
                }
                if (e is AccountInstanceMismatchException) {
                    onInstanceMismatch(e.accountInstance, e.apiInstance)
                    return
                }
                Log.d(TAG, "Vote failed", e)
            }
        },
    )
}

@JsonClass(generateAdapter = true, generator = "sealed:t")
sealed interface VotableRef {
    @JsonClass(generateAdapter = true)
    @TypeLabel("1")
    data class PostRef(
        val postId: Int,
    ) : VotableRef

    @JsonClass(generateAdapter = true)
    @TypeLabel("2")
    data class CommentRef(
        val commentId: Int,
    ) : VotableRef
}

fun PostView.toVotableRef() =
    VotableRef.PostRef(
        this.post.id,
    )

fun CommentView.toVotableRef() =
    VotableRef.CommentRef(
        this.comment.id,
    )
