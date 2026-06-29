package com.idunnololz.summit.api.dto.lemmy

data class ListPrivateMessageReportsResponse(
  val private_message_reports: List<PrivateMessageReportView>,
  val prev_page: String? = null,
  val next_page: String? = null,
)
