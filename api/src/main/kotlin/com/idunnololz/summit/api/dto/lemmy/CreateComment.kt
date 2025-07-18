package com.idunnololz.summit.api.dto.lemmy

data class CreateComment(
  val content: String,
  val post_id: PostId,
  val parent_id: CommentId? = null,
  val language_id: LanguageId? = null,
  val form_id: String? = null,
  val auth: String,
)
