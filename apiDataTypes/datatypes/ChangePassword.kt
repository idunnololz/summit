package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class ChangePassword(
    val new_password: String,
    val new_password_verify: String,
    val old_password: String,
)
