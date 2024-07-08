package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class MarkPostAsRead(
    val post_id: PostId? = null,
    val post_ids: List<PostId>? = null,
    val read: Boolean,
)
