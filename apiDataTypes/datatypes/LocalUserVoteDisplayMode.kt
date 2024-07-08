package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class LocalUserVoteDisplayMode(
    val local_user_id: LocalUserId,
    val score: Boolean,
    val upvotes: Boolean,
    val downvotes: Boolean,
    val upvote_percentage: Boolean,
)
