package com.idunnololz.summit.inbox.conversation

import android.os.Parcelable
import com.idunnololz.summit.api.dto.lemmy.PersonId
import com.idunnololz.summit.api.dto.lemmy.PrivateMessageView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.inbox.LiteInboxItem
import com.idunnololz.summit.util.dateStringToTs
import kotlinx.parcelize.Parcelize

@Parcelize
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
) : LiteInboxItem, Parcelable {
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
