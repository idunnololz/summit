package com.idunnololz.summit.api.dto.other

import com.idunnololz.summit.api.dto.lemmy.v4.models.NotificationTypeFilter

class ListInboxArgs(
  val limit: Double?,
  val page_cursor: String?,
  val creator_id: Double?,
  val unread_only: Boolean?,
  val type: NotificationTypeFilter?
)