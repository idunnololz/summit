package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
data class ListCommentLikesResponse(
    val comment_likes: List<VoteView>,
)
