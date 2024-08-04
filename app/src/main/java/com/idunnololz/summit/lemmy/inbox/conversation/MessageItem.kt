package com.idunnololz.summit.lemmy.inbox.conversation

import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PrivateMessageView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.lemmy.inbox.LiteInboxItem
import com.idunnololz.summit.util.dateStringToTs

data class MessageItem(
    override val id: Int,
    val authorId: PersonId,
    val authorName: String,
    val authorInstance: String,
    val authorAvatar: String?,
    val title: String,
    val content: String,
    val lastUpdate: String,
    override val lastUpdateTs: Long,
    val isDeleted: Boolean,
    val isRead: Boolean,
    val targetUserName: String?,
) : LiteInboxItem {
    override fun updateIsRead(isRead: Boolean): LiteInboxItem = copy(isRead = isRead)
}

fun PrivateMessageView.toMessageItem() = MessageItem(
    id = private_message.id,
    authorId = creator.id,
    authorName = creator.name,
    authorInstance = creator.instance,
    authorAvatar = creator.avatar,
    title = creator.name,
    content = private_message.content,
    lastUpdate = private_message.updated ?: private_message.published,
    lastUpdateTs = dateStringToTs(
        private_message.updated
            ?: private_message.published,
    ),
    isDeleted = private_message.deleted,
    isRead = private_message.read,
    targetUserName = recipient.name,
)
