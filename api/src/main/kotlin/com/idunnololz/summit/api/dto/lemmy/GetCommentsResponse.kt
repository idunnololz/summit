package com.idunnololz.summit.api.dto.lemmy

data class GetCommentsResponse(
  val comments: List<CommentView>,
  val next_page: String? = null,
  val prev_page: String? = null,
)
