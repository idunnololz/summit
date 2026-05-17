package com.idunnololz.summit.localTracking

import com.idunnololz.summit.lemmy.CommunityRef

interface OnTrackingEventListener {
  fun onDeleteAll()
  fun onViewCommunity(communityRef: CommunityRef)
  fun onVote(userId: Long, targetPersonId: Long, postId: Long?, commentId: Long?, direction: Int)
}

open class SimpleOnTrackingEventListener : OnTrackingEventListener {
  override fun onDeleteAll() {}

  override fun onViewCommunity(communityRef: CommunityRef) {}

  override fun onVote(
    userId: Long,
    targetPersonId: Long,
    postId: Long?,
    commentId: Long?,
    direction: Int,
  ) {
  }
}
