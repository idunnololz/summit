package com.idunnololz.summit.lemmy.inbox.conversation

import android.os.Parcelable
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.lemmy.inbox.db.ConversationEntry
import kotlinx.parcelize.Parcelize

data class ConversationsModel(
    val conversations: List<Conversation> = listOf(),
    val drafts: Map<PersonId, DbMessageDraft> = mapOf(),
    val conversationEarliestMessageTs: Long? = null,
    val isLoaded: Boolean = true,
)

@Parcelize
data class Conversation(
    val id: Long,
    val ts: Long,
    val accountStableId: String,
    val personId: Long, // the person you are conversing with
    val personInstance: String,
    val personName: String?,
    val iconUrl: String?,
    val content: String?, // Usually the last message sent
    val isRead: Boolean,
    val mostRecentMessageId: Long?,
) : Parcelable

fun ConversationEntry.toConversation() = Conversation(
    id = id,
    ts = ts,
    accountStableId = accountStableId,
    personId = personId,
    personInstance = personInstance,
    personName = personName,
    iconUrl = iconUrl,
    content = content,
    isRead = isRead,
    mostRecentMessageId = mostRecentMessageId,
)

fun Conversation.toEntry() = ConversationEntry(
    id = id,
    ts = ts,
    accountStableId = accountStableId,
    personId = personId,
    personInstance = personInstance,
    personName = personName,
    title = "",
    iconUrl = iconUrl,
    content = content,
    isRead = isRead,
    mostRecentMessageId = mostRecentMessageId,
)
