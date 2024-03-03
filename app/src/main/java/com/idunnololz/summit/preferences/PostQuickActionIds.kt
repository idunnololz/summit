package com.idunnololz.summit.preferences

typealias PostQuickActionId = Int

object PostQuickActionIds {
    const val Voting: PostQuickActionId = 1001
    const val Reply: PostQuickActionId = 1002
    const val Save: PostQuickActionId = 1003
    const val Share: PostQuickActionId = 1004
    const val TakeScreenshot: PostQuickActionId = 1005
    const val CrossPost: PostQuickActionId = 1006
    const val ShareSourceLink: PostQuickActionId = 1007
    const val CommunityInfo: PostQuickActionId = 1008
    const val ViewSource: PostQuickActionId = 1009
    const val DetailedView: PostQuickActionId = 1010

    const val More: PostQuickActionId = 1999

    val AllActions = listOf(
        Voting,
        Reply,
        Save,
        Share,
        TakeScreenshot,
        CrossPost,
        ShareSourceLink,
        CommunityInfo,
        ViewSource,
        DetailedView,
//                More, More isn't optional...
    )
}
