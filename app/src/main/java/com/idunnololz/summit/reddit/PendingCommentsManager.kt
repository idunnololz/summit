package com.idunnololz.summit.reddit

import android.content.Context
import com.idunnololz.summit.reddit_objects.RedditCommentItem

/**
 * There is a delay between when comments are posted and the API returning it as part of get listing
 *
 * This manager is a bridge to temp. show the comment before it's returned by API.
 */
class PendingCommentsManager(
    private val context: Context
) {

    companion object {
        lateinit var instance: PendingCommentsManager

        fun initialize(context: Context) {
            instance = PendingCommentsManager(context)
        }
    }

    /**
     * Key is the comment id.
     */
    private val pendingCommentsDict = hashMapOf<String, RedditCommentItem>()
    private val parentIdToComments = hashMapOf<String, ArrayList<RedditCommentItem>>()

    fun addPendingComment(pendingCommentItem: RedditCommentItem) {
        val oldValue = pendingCommentsDict.put(pendingCommentItem.name, pendingCommentItem)
        if (oldValue != null) {
            parentIdToComments.getOrPut(pendingCommentItem.parentId) { arrayListOf() }
                .removeAll { it.name == pendingCommentItem.name }
        }
        parentIdToComments.getOrPut(pendingCommentItem.parentId) { arrayListOf() }
            .add(pendingCommentItem)
    }

    fun getPendingComments(parentId: String): List<RedditCommentItem>? =
        parentIdToComments[parentId]

    fun removePendingComment(realCommentItem: RedditCommentItem) {
        val pendingComment = pendingCommentsDict[realCommentItem.name]

        if (pendingComment != null) {
            if (realCommentItem.createdUtc >= pendingComment.createdUtc) {
                val old = pendingCommentsDict.remove(realCommentItem.name)
                if (old != null) {
                    parentIdToComments.getOrPut(realCommentItem.parentId) { arrayListOf() }
                        .removeAll { it.name == realCommentItem.name }
                }
            }
        }
    }

}