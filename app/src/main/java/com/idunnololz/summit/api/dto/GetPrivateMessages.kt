package com.idunnololz.summit.api.dto

data class GetPrivateMessages(
    val unread_only: Boolean? = null,
    val page: Int? = null,
    val limit: Int? = null,
    val creator_id: PersonId? = null,
    val auth: String,
)
