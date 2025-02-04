package com.idunnololz.summit.lemmy.inbox.inbox

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

    data class RegularInboxItem(
        override val page: Int,
        val item: InboxItem,
    ) : InboxListItem

    data class ConversationItem(
        override val page: Int,
        val conversation: Conversation,
        val draftMessage: DraftData.MessageDraftData?,
    ) : InboxListItem
}
