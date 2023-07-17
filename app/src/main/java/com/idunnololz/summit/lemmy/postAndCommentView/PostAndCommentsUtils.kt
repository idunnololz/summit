package com.idunnololz.summit.lemmy.postAndCommentView

import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.lemmy.post.ModernThreadLinesDecoration
import com.idunnololz.summit.lemmy.post.OldThreadLinesDecoration
import com.idunnololz.summit.preferences.CommentsThreadStyle
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.ext.clearItemDecorations

fun RecyclerView.setupForPostAndComments(preferences: Preferences) {
    clearItemDecorations()
    addItemDecoration(
        when (preferences.commentThreadStyle) {
            CommentsThreadStyle.Legacy ->
                OldThreadLinesDecoration(context, preferences.hideCommentActions)
            else -> {
                ModernThreadLinesDecoration(context, preferences.hideCommentActions)
            }
        }
    )
}