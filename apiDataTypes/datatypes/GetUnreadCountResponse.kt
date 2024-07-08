package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class GetUnreadCountResponse(
    val replies: Long,
    val mentions: Long,
    val private_messages: Long,
)
