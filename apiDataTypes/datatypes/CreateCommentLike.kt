package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class CreateCommentLike(
    val comment_id: CommentId,
    val score: Long,
)
