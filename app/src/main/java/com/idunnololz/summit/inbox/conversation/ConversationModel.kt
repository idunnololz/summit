package com.idunnololz.summit.inbox.conversation

data class ConversationModel(
  val accountId: Long?,
  val allMessages: List<MessageItem>,
  val nextPageIndex: Int,
  val hasMore: Boolean,
)
