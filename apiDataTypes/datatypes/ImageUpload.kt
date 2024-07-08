package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class ImageUpload(
    val local_user_id: LocalUserId,
    val pictrs_alias: String,
    val pictrs_delete_token: String,
    val published: String,
)
