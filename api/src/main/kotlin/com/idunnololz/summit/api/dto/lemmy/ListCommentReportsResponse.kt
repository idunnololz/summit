package com.idunnololz.summit.api.dto.lemmy

data class ListCommentReportsResponse(
  val comment_reports: List<CommentReportView>,
  val prev_page: String? = null,
  val next_page: String? = null,
)
