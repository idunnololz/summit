package com.idunnololz.summit.inbox

interface LiteInboxItem {
  val id: Int
  val lastUpdateTs: Long

  fun updateIsRead(isRead: Boolean): LiteInboxItem
}
