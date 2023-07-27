package com.idunnololz.summit.lemmy.utils

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.actions.LemmySwipeActionCallback
import com.idunnololz.summit.lemmy.community.usesDividers
import com.idunnololz.summit.preferences.CommentGestureAction
import com.idunnololz.summit.preferences.PostGestureAction
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.VerticalSpaceItemDecoration
import com.idunnololz.summit.util.ext.clearItemDecorations
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.getDrawableCompat

fun RecyclerView.setupDecoratorsForPostList(preferences: Preferences) {
    clearItemDecorations()

    if (preferences.getPostsLayout().usesDividers()) {
        this.addItemDecoration(
            CustomDividerItemDecoration(
                this.context,
                DividerItemDecoration.VERTICAL,
            ).apply {
                setDrawable(
                    checkNotNull(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.vertical_divider,
                        ),
                    ),
                )
            },
        )
    } else {
        this.addItemDecoration(
            VerticalSpaceItemDecoration(
                this.context.getDimen(R.dimen.padding),
                true,
            ),
        )
    }
}

fun Preferences.getPostSwipeActions(context: Context): List<LemmySwipeActionCallback.SwipeAction> {
    fun Int.toIcon() =
        when (this) {
            PostGestureAction.Upvote ->
                R.drawable.baseline_arrow_upward_24
            PostGestureAction.Downvote ->
                R.drawable.baseline_arrow_downward_24
            PostGestureAction.Bookmark ->
                R.drawable.baseline_bookmark_add_24
            PostGestureAction.Hide ->
                R.drawable.baseline_hide_24
            PostGestureAction.MarkAsRead ->
                R.drawable.baseline_check_24
            PostGestureAction.Reply ->
                R.drawable.baseline_reply_24
            else ->
                R.drawable.baseline_close_24
        }

    return listOf(
        LemmySwipeActionCallback.SwipeAction(
            postGestureAction1,
            context.getDrawableCompat(postGestureAction1.toIcon())!!.mutate(),
            context.getColorCompat(R.color.style_red),
        ),
        LemmySwipeActionCallback.SwipeAction(
            postGestureAction2,
            context.getDrawableCompat(postGestureAction2.toIcon())!!.mutate(),
            context.getColorCompat(R.color.style_blue),
        ),
        LemmySwipeActionCallback.SwipeAction(
            postGestureAction3,
            context.getDrawableCompat(postGestureAction3.toIcon())!!.mutate(),
            context.getColorCompat(R.color.style_amber),
        ),
    ).filter { it.id != PostGestureAction.None }
}

fun Preferences.getCommentSwipeActions(context: Context): List<LemmySwipeActionCallback.SwipeAction> {
    fun Int.toIcon() =
        when (this) {
            CommentGestureAction.Upvote ->
                R.drawable.baseline_arrow_upward_24
            CommentGestureAction.Downvote ->
                R.drawable.baseline_arrow_downward_24
            CommentGestureAction.Bookmark ->
                R.drawable.baseline_bookmark_add_24
            CommentGestureAction.Reply ->
                R.drawable.baseline_reply_24
            CommentGestureAction.CollapseOrExpand ->
                R.drawable.baseline_unfold_less_24
            else ->
                R.drawable.baseline_close_24
        }

    return listOf(
        LemmySwipeActionCallback.SwipeAction(
            commentGestureAction1,
            context.getDrawableCompat(commentGestureAction1.toIcon())!!.mutate(),
            context.getColorCompat(R.color.style_red),
        ),
        LemmySwipeActionCallback.SwipeAction(
            commentGestureAction2,
            context.getDrawableCompat(commentGestureAction2.toIcon())!!.mutate(),
            context.getColorCompat(R.color.style_blue),
        ),
        LemmySwipeActionCallback.SwipeAction(
            commentGestureAction3,
            context.getDrawableCompat(commentGestureAction3.toIcon())!!.mutate(),
            context.getColorCompat(R.color.style_amber),
        ),
    ).filter { it.id != CommentGestureAction.None }
}
