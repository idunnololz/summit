package com.idunnololz.summit.api.dto.lemmy

data class ListCommentReportsResponse(
  val comment_reports: List<CommentReportView>,
)
