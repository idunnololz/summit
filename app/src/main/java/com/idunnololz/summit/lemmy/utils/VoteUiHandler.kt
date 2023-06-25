package com.idunnololz.summit.lemmy.utils

import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.api.AccountInstanceMismatchException
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

private val TAG = "VoteUiHandler"

interface VoteUiHandler {
    fun bindVoteUi(
        lifecycleOwner: LifecycleOwner,
        currentScore: Int,
        instance: String,
        ref: VotableRef,
        upVoteView: View,
        downVoteView: View,
        registration: AccountActionsManager.Registration
    )
    fun unbindVoteUi(upVoteView: View)
}

fun VoteUiHandler.bind(
    lifecycleOwner: LifecycleOwner,
    instance: String,
    commentView: CommentView,
    upVoteView: ImageView,
    downVoteView: ImageView,
    onSignInRequired: () -> Unit,
    onInstanceMismatch: (String, String) -> Unit,
) {
    bind(
        lifecycleOwner,
        instance,
        commentView.my_vote ?: 0,
        VotableRef.CommentRef(commentView.comment.id),
        upVoteView,
        downVoteView,
        onSignInRequired,
        onInstanceMismatch,
    )
}

fun VoteUiHandler.bind(
    lifecycleOwner: LifecycleOwner,
    instance: String,
    postView: PostView,
    upVoteView: ImageView,
    downVoteView: ImageView,
    onSignInRequired: () -> Unit,
    onInstanceMismatch: (String, String) -> Unit,
) {
    bind(
        lifecycleOwner,
        instance,
        postView.my_vote ?: 0,
        VotableRef.PostRef(postView.post.id),
        upVoteView,
        downVoteView,
        onSignInRequired,
        onInstanceMismatch,
    )
}

fun VoteUiHandler.bind(
    lifecycleOwner: LifecycleOwner,
    instance: String,
    currentScore: Int,
    ref: VotableRef,
    upVoteView: ImageView,
    downVoteView: ImageView,
    onSignInRequired: () -> Unit,
    onInstanceMismatch: (String, String) -> Unit,
) {
    val context = upVoteView.context
    fun update(score: Int) {
        if (score < 0) {
            upVoteView.setColorFilter(context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
            downVoteView.setColorFilter(ContextCompat.getColor(context, R.color.downvoteColor))
        } else if (score > 0) {
            upVoteView.setColorFilter(ContextCompat.getColor(context, R.color.upvoteColor))
            downVoteView.setColorFilter(context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
        } else {
            upVoteView.setColorFilter(context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
            downVoteView.setColorFilter(context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
        }

        upVoteView.invalidate()
        downVoteView.invalidate()
    }
    bindVoteUi(
        lifecycleOwner,
        currentScore,
        instance,
        ref,
        upVoteView,
        downVoteView,
        object : AccountActionsManager.Registration {
            override fun voteCurrent(score: Int) {
                update(score)
            }

            override fun voteSuccess(newScore: Int) {
                update(newScore)
            }

            override fun votePending(pendingScore: Int) {
                update(pendingScore)
            }

            override fun voteFailed(score: Int, e: Throwable) {
                update(score)

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

        }
    )
}

@JsonClass(generateAdapter = true, generator = "sealed:t")
sealed interface VotableRef {
    @JsonClass(generateAdapter = true)
    @TypeLabel("1")
    data class PostRef(
        val postId: Int
    ) : VotableRef

    @JsonClass(generateAdapter = true)
    @TypeLabel("2")
    data class CommentRef(
        val commentId: Int
    ) : VotableRef
}