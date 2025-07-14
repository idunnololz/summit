package com.idunnololz.summit.api.dto.lemmy

data class ListPrivateMessageReportsResponse(
  val private_message_reports: List<PrivateMessageReportView>,
)
