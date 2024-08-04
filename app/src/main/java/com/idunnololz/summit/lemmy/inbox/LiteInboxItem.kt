package com.idunnololz.summit.lemmy.inbox

interface LiteInboxItem {
    val id: Int
    val lastUpdateTs: Long

    fun updateIsRead(isRead: Boolean): LiteInboxItem
}
