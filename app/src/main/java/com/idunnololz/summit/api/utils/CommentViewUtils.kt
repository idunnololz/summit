package com.idunnololz.summit.api.utils

import com.idunnololz.summit.api.dto.Comment
import com.idunnololz.summit.api.dto.CommentView

fun Comment.getDepth(): Int {
    val depth = path.split(".").size.minus(2)
    return Integer.max(depth, 0)
}

fun CommentView.getDepth(): Int =
    comment.getDepth()

fun CommentView.getUniqueKey(): String =
    "comment_${comment.id}"