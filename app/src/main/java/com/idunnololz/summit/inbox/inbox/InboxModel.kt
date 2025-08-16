package com.idunnololz.summit.inbox.inbox

import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.inbox.InboxItem
import com.idunnololz.summit.inbox.conversation.Conversation

class InboxModel(
  val items: List<InboxListItem> = listOf(),
  val earliestMessageTs: Long? = null,
  val hasMore: Boolean = false,
)

sealed interface InboxListItem {

  val page: Int
  val id: Long
  val read: Boolean

  data class RegularInboxItem(
    override val page: Int,
    val item: InboxItem,
  ) : InboxListItem {
    override val id: Long
      get() = item.id.toLong()
    override val read: Boolean
      get() = item.isRead
  }

  data class ConversationItem(
    override val page: Int,
    val conversation: Conversation,
    val draftMessage: DraftData.MessageDraftData?,
  ) : InboxListItem {
    override val id: Long
      get() = conversation.id
    override val read: Boolean
      get() = conversation.isRead
  }

  data class RegistrationApplicationInboxItem(
    override val page: Int,
    val item: InboxItem.RegistrationApplicationInboxItem,
  ) : InboxListItem {
    override val id: Long
      get() = item.id.toLong()
    override val read: Boolean
      get() = item.isRead
  }
}
