package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class GetPrivateMessages(
    val unread_only: Boolean? = null,
    val page: Long? = null,
    val limit: Long? = null,
    val creator_id: PersonId? = null,
)
