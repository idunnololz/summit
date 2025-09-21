package com.idunnololz.summit.api.dto.lemmy

data class ResolvePrivateMessageReport(
  val report_id: PrivateMessageReportId,
  val resolved: Boolean,
)
