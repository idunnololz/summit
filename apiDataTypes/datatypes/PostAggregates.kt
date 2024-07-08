package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class PostAggregates(
    val post_id: PostId,
    val comments: Long,
    val score: Long,
    val upvotes: Long,
    val downvotes: Long,
    val published: String,
    val newest_comment_time: String,
)
