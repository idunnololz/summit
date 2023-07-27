package com.idunnololz.summit.preferences

typealias CommentActionId = Int

object CommentGestureAction {
    const val None: CommentActionId = 0
    const val Upvote: CommentActionId = 1
    const val Downvote: CommentActionId = 2
    const val Reply: CommentActionId = 3
    const val Bookmark: CommentActionId = 6
    const val CollapseOrExpand: CommentActionId = 7
}
