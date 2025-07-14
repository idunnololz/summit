package com.idunnololz.summit.api.dto.lemmy

data class ListCommentLikes(
  val comment_id: CommentId,
  val page: Long? = null,
  val limit: Long? = null,
)
