package com.idunnololz.summit.api.dto.lemmy

data class MarkPersonMentionAsRead(
  val person_mention_id: PersonMentionId,
  val read: Boolean,
  val auth: String,
)
