package com.idunnololz.summit.api.dto.lemmy

data class CommunityView(
  val community: Community,
  val subscribed: SubscribedType /* "Subscribed" | "NotSubscribed" | "Pending" */,
  val blocked: Boolean,
  val counts: CommunityAggregates,
)
