package com.idunnololz.summit.reddit

import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.idunnololz.summit.R
import com.idunnololz.summit.auth.RedditAuthManager
import com.idunnololz.summit.reddit_actions.ActionInfo
import com.idunnololz.summit.util.Status
import com.idunnololz.summit.util.ext.getColorFromAttribute

object UserActionsHelper {

    fun setupActions(
        id: String,
        likes: Boolean?,
        lifecycleOwner: LifecycleOwner,
        fragmentManager: FragmentManager,
        upvoteButton: ImageView,
        downvoteButton: ImageView,
        onVoteChangedFn: (Int) -> Unit
    ) {
        val context = upvoteButton.context

        fun vote(dir: Int) {
            val notSignedIn = RedditAuthManager.instance.showPreSignInIfNeeded(fragmentManager)

            if (notSignedIn) return

            onVoteChangedFn(dir)

            PendingActionsManager.instance.voteOn(id, dir, lifecycleOwner) {
                when (it.status) {
                    Status.LOADING -> {
                    }
                    Status.SUCCESS -> {
                        onVoteChangedFn((it.data.info as ActionInfo.VoteActionInfo).dir)
                    }
                    Status.FAILED -> {
                    }
                }
            }
        }

        upvoteButton.setOnClickListener {
            if (likes == true) {
                vote(0)
            } else {
                vote(1)
            }
        }
        downvoteButton.setOnClickListener {
            if (likes == false) {
                vote(0)
            } else {
                vote(-1)
            }
        }

        if (likes == true) {
            upvoteButton.setColorFilter(ContextCompat.getColor(context, R.color.upvoteColor))
            downvoteButton.setColorFilter(context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
        } else if (likes == false) {
            upvoteButton.setColorFilter(context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
            downvoteButton.setColorFilter(ContextCompat.getColor(context, R.color.downvoteColor))
        } else {
            upvoteButton.setColorFilter(context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
            downvoteButton.setColorFilter(context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
        }
        upvoteButton.invalidate()
        downvoteButton.invalidate()
    }
}