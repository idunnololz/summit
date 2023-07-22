package com.idunnololz.summit.api.utils

import android.net.Uri
import com.idunnololz.summit.api.dto.Comment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView

fun Comment.getDepth(): Int {
    val depth = path.split(".").size.minus(2)
    return Integer.max(depth, 0)
}

fun CommentView.getDepth(): Int =
    comment.getDepth()

fun CommentView.getUniqueKey(): String =
    "comment_${comment.id}"

val CommentView.instance: String
    get() = Uri.parse(this.post.ap_id).host
        ?: Uri.parse(this.comment.ap_id).host
        ?: this.community.instance