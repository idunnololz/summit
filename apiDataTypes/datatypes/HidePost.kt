package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class HidePost(
    val post_ids: List<PostId>,
    val hide: Boolean,
)
