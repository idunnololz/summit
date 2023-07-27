package com.idunnololz.summit.preferences

typealias PostActionId = Int

object PostGestureAction {
    const val None: PostActionId = 0
    const val Upvote: PostActionId = 1
    const val Downvote: PostActionId = 2
    const val Reply: PostActionId = 3
    const val MarkAsRead: PostActionId = 4
    const val Hide: PostActionId = 5
    const val Bookmark: PostActionId = 6
}
