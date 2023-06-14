package com.idunnololz.summit.reddit

import android.content.Context
import com.idunnololz.summit.reddit_objects.RedditCommentItem

class PendingEditsManager(
    private val context: Context
) {

    companion object {
        lateinit var instance: PendingEditsManager

        fun initialize(context: Context) {
            instance = PendingEditsManager(context)
        }
    }

    private val pendingCommentEdits: HashMap<String, RedditCommentItem> = hashMapOf()

    fun addPendingCommentEdit(item: RedditCommentItem) {
        pendingCommentEdits[item.name] = item
    }

    fun getPendingCommentEdit(name: String): RedditCommentItem? =
        pendingCommentEdits[name]

    fun removePendingEdit(realCommentItem: RedditCommentItem) {
        val pendingComment = pendingCommentEdits[realCommentItem.name]

        if (pendingComment != null) {
            if (realCommentItem.edited is Double &&
                realCommentItem.edited >= (pendingComment.edited as? Double) ?: 0.0
            ) {
                pendingCommentEdits.remove(realCommentItem.name)
            }
        }
    }
}