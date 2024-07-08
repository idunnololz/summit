package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class CommentAggregates(
    val comment_id: CommentId,
    val score: Long,
    val upvotes: Long,
    val downvotes: Long,
    val published: String,
    val child_count: Long,
)
