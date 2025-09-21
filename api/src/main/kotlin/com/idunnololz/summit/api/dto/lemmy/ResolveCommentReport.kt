package com.idunnololz.summit.api.dto.lemmy

data class ResolveCommentReport(
  val report_id: CommentReportId,
  val resolved: Boolean,
)
