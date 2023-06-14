package com.idunnololz.summit.reddit.ext

import android.text.Spanned
import android.util.Log
import com.idunnololz.summit.reddit.PendingCommentsManager
import com.idunnololz.summit.reddit.PendingEditsManager
import com.idunnololz.summit.reddit_objects.*
import com.idunnololz.summit.util.Utils

private const val TAG = "RedditExt"

fun ListingItem.getType(): ListingItemType =
    if (domain == "i.redd.it") {
        ListingItemType.REDDIT_IMAGE
    } else if (domain == "v.redd.it") {
        ListingItemType.REDDIT_VIDEO
    } else if (galleryData != null && galleryData.items.isNotEmpty()) {
        ListingItemType.REDDIT_GALLERY
    } else if (thumbnail == "default" || thumbnail == "self" || domain.startsWith("self")) {
        ListingItemType.DEFAULT_SELF
    } else {
        Log.e(TAG, "UNKNOWN TYPE. Domain: ${domain}")
        ListingItemType.UNKNOWN
    }

/**
 * @return true if the domain (and probably the url) links to the reddit post itself (self link)
 */
fun ListingItem.isDomainSelf(): Boolean =
    domain.startsWith("self")

enum class ListingItemType {
    DEFAULT_SELF,
    REDDIT_IMAGE,
    REDDIT_VIDEO,
    REDDIT_GALLERY,
    UNKNOWN
}

fun ListingItem.getPreviewUrl(): String? = preview?.images?.first()?.source?.getUrl()?.let {
    Utils.fromHtml(it).toString()
} ?: thumbnail

fun ListingItem.getFormattedTitle(): Spanned = Utils.fromHtml(title)

sealed class PostItem(
    val name: String
) {
    data class ContentItem(
        val listingItem: ListingItem
    ) : PostItem(listingItem.name)

    data class CommentItem(
        val commentItem: RedditCommentItem,
        val depth: Int,
        val isPending: Boolean
    ) : PostItem(commentItem.name)

    data class MoreCommentsItem(
        val moreItem: MoreItem,
        val depth: Int,
        val linkId: String
    ) : PostItem(moreItem.name)
}

fun List<RedditObject>.getFirstListingItem(): ListingItem? {
    forEach {
        if (it.kind == "t3") {
            return (it as ListingItemObject).data
        }
    }
    return null
}

fun List<RedditObject>.flattenPostData(moreCommentsMap: Map<String, List<CommentItemObject>> = mapOf()): List<PostItem> {
    val redditObjects = this
    var lastT3Id: String? = null
    val pendingCommentsManager = PendingCommentsManager.instance
    val pendingEditsManager = PendingEditsManager.instance

    fun processDataRec(
        o: RedditObject?,
        pendingItems: List<RedditCommentItem>?,
        items: MutableList<PostItem>,
        depth: Int = 0
    ) {
        fun processCommentItem(it: RedditCommentItem, isPending: Boolean) {
            if (!isPending) {
                pendingCommentsManager.removePendingComment(it)
            }
            pendingEditsManager.removePendingEdit(it)

            if (it.collapsed) {
                items.add(
                    PostItem.CommentItem(
                        it,
                        depth,
                        isPending
                    )
                )
            } else {
                val pendingEdit = pendingEditsManager.getPendingCommentEdit(it.name)
                if (pendingEdit != null) {
                    items.add(
                        PostItem.CommentItem(
                            pendingEdit,
                            depth,
                            true
                        )
                    )
                } else {
                    items.add(
                        PostItem.CommentItem(
                            it,
                            depth,
                            isPending
                        )
                    )
                }

                it.replies?.let { replies ->
                    processDataRec(
                        replies,
                        pendingCommentsManager.getPendingComments(it.name),
                        items,
                        depth + 1
                    )
                }
            }
        }
        pendingItems?.forEach {
            processCommentItem(it, true)
        }

        o ?: return

        when (o.kind) {
            "Listing" -> {
                (o as ListingObject).data?.children?.forEach {
                    processDataRec(it, null, items, depth)
                }
            }
            "t3" -> {
                (o as ListingItemObject).data?.let {
                    lastT3Id = it.name
                    items.add(PostItem.ContentItem(it))
                    processDataRec(
                        null,
                        pendingCommentsManager.getPendingComments(it.name),
                        items,
                        depth
                    )
                }
            }
            "t1" -> {
                (o as CommentItemObject).data?.let {
                    processCommentItem(it, false)
                }
            }
            "more" -> {
                (o as MoreItemObject).data?.let { moreItem ->
                    val moreComments = moreCommentsMap[moreItem.parentId]

                    if (moreComments != null) {
                        moreComments.forEach {
                            processDataRec(
                                it,
                                pendingCommentsManager.getPendingComments(moreItem.parentId),
                                items,
                                depth
                            )
                        }
                    } else {
                        if (moreItem.children.isEmpty()) {
                            return
                        }
                        lastT3Id?.let {
                            items.add(
                                PostItem.MoreCommentsItem(
                                    moreItem,
                                    depth,
                                    it
                                )
                            )
                        }
                    }
                }
            }
            else -> {
                Log.e(TAG, "Unknown kind ${o.kind}")
            }
        }
    }

    return ArrayList<PostItem>().apply {
        for (o in redditObjects) {
            processDataRec(o, null, this)
        }
    }
}