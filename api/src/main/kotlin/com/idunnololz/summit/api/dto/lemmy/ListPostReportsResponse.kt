package com.idunnololz.summit.api.dto.lemmy

data class ListPostReportsResponse(
  val post_reports: List<PostReportView>,
  val prev_page: String? = null,
  val next_page: String? = null,
)
