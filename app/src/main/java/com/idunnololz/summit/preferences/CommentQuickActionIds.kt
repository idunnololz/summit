package com.idunnololz.summit.preferences

typealias CommentQuickActionId = Int

object CommentQuickActionIds {
    const val Voting: CommentQuickActionId = 1
    const val Reply: CommentQuickActionId = 2
    const val Save: CommentQuickActionId = 3
    const val Share: CommentQuickActionId = 4
    const val TakeScreenshot: CommentQuickActionId = 5
    const val ShareSource: CommentQuickActionId = 6
    const val OpenComment: CommentQuickActionId = 7
    const val ViewSource: CommentQuickActionId = 8
    const val DetailedView: CommentQuickActionId = 9

    const val More: CommentQuickActionId = 999

    val AllActions = listOf(
        Voting,
        Reply,
        Save,
        Share,
        TakeScreenshot,
        ShareSource,
        OpenComment,
        ViewSource,
        DetailedView,
        More,
//                CommentQuickActionId.More, More isn't optional...
    )
}
