package com.idunnololz.summit.api.dto.piefed

import com.google.gson.annotations.SerializedName

class GetPrivateMessageConversation(
  @SerializedName("person_id")
  val personId: kotlin.Long? = null,
  @SerializedName("conversation_id")
  val conversationId: kotlin.Int? = null,
  @SerializedName("page")
  val page: kotlin.Int? = null,
  @SerializedName("limit")
  val limit: kotlin.Int? = null,
)