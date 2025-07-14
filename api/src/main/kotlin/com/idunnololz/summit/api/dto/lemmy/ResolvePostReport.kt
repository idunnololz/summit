package com.idunnololz.summit.api.dto.lemmy

data class ResolvePostReport(
  val report_id: PostReportId,
  val resolved: Boolean,
  val auth: String,
)
