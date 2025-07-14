package com.idunnololz.summit.api.dto.lemmy

data class PersonMention(
  val id: PersonMentionId,
  val recipient_id: PersonId,
  val comment_id: CommentId,
  val read: Boolean,
  val published: String,
)
