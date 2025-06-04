package com.idunnololz.summit.lemmy.inbox.inbox

import com.idunnololz.summit.api.dto.RegistrationApplication
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.conversation.Conversation

class InboxModel(
  val items: List<InboxListItem> = listOf(),
  val earliestMessageTs: Long? = null,
  val hasMore: Boolean = false,
)

sealed interface InboxListItem {

  val page: Int
  val id: Long

  data class RegularInboxItem(
    override val page: Int,
    val item: InboxItem,
  ) : InboxListItem {
    override val id: Long
      get() = item.id.toLong()
  }

  data class ConversationItem(
    override val page: Int,
    val conversation: Conversation,
    val draftMessage: DraftData.MessageDraftData?,
  ) : InboxListItem {
    override val id: Long
      get() = conversation.id
  }

  data class RegistrationApplicationInboxItem(
    override val page: Int,
    val item: InboxItem.RegistrationApplicationInboxItem,
  ) : InboxListItem {
    override val id: Long
      get() = item.id.toLong()
  }
}
