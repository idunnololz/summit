package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class CommentReport(
    val id: CommentReportId,
    val creator_id: PersonId,
    val comment_id: CommentId,
    val original_comment_text: String,
    val reason: String,
    val resolved: Boolean,
    val resolver_id: PersonId? = null,
    val published: String,
    val updated: String? = null,
)
