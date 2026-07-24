package com.idunnololz.summit.api.dto.other

import com.idunnololz.summit.api.dto.lemmy.v4.models.NotificationTypeFilter

class ListInboxArgs(
  val limit: Int?,
  val page_cursor: String?,
  val creator_id: Int?,
  val unread_only: Boolean?,
  val type: NotificationTypeFilter?,
)
