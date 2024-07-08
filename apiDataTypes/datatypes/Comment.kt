package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class Comment(
    val id: CommentId,
    val creator_id: PersonId,
    val post_id: PostId,
    val content: String,
    val removed: Boolean,
    val published: String,
    val updated: String? = null,
    val deleted: Boolean,
    val ap_id: String,
    val local: Boolean,
    val path: String,
    val distinguished: Boolean,
    val language_id: LanguageId,
)
