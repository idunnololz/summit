package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class GetReportCountResponse(
    val community_id: CommunityId? = null,
    val comment_reports: Long,
    val post_reports: Long,
    val private_message_reports: Long? = null,
)
