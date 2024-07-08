package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class PrivateMessageReport(
    val id: PrivateMessageReportId,
    val creator_id: PersonId,
    val private_message_id: PrivateMessageId,
    val original_pm_text: String,
    val reason: String,
    val resolved: Boolean,
    val resolver_id: PersonId? = null,
    val published: String,
    val updated: String? = null,
)
