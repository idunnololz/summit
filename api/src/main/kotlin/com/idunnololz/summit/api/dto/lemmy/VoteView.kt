package com.idunnololz.summit.api.dto.lemmy

data class VoteView(
  val creator: Person,
  val creator_banned_from_community: Boolean,
  val score: Long,
)
