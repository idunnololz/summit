package com.idunnololz.summit.preferences

typealias PostInFeedQuickActionId = Int

object PostInFeedQuickActionIds {
  const val Voting: PostInFeedQuickActionId = 1001
  const val Reply: PostInFeedQuickActionId = 1002
  const val Save: PostInFeedQuickActionId = 1003
  const val Share: PostInFeedQuickActionId = 1004
  const val TakeScreenshot: PostInFeedQuickActionId = 1005
  const val CrossPost: PostInFeedQuickActionId = 1006
  const val ShareSourceLink: PostInFeedQuickActionId = 1007
  const val CommunityInfo: PostInFeedQuickActionId = 1008
  const val ViewSource: PostInFeedQuickActionId = 1009
  const val DetailedView: PostInFeedQuickActionId = 1010
  const val MarkAsRead: PostInFeedQuickActionId = 1011
  const val HidePost: PostInFeedQuickActionId = 1012

  val AllActions = listOf(
    Voting,
    Reply,
    Save,
    Share,
    CrossPost,
    ShareSourceLink,
    CommunityInfo,
    ViewSource,
    DetailedView,
    MarkAsRead,
    HidePost,
  )
}
