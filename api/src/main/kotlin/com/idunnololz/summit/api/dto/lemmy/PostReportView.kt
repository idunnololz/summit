package com.idunnololz.summit.api.dto.lemmy

data class PostReportView(
  val post_report: PostReport,
  val post: Post,
  val community: Community,
  val creator: Person,
  val post_creator: Person,
  val creator_banned_from_community: Boolean,
  val my_vote: Int? = null,
  val counts: PostAggregates,
  val resolver: Person? = null,
)
