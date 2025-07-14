package com.idunnololz.summit.api.dto.lemmy

data class GetRepliesResponse(
  val replies: List<CommentReplyView>,
)
