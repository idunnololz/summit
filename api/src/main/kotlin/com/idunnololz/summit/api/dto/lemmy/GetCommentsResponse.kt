package com.idunnololz.summit.api.dto.lemmy

data class GetCommentsResponse(
  val comments: List<CommentView>,
)
