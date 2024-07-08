package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class PasswordChangeAfterReset(
    val token: String,
    val password: String,
    val password_verify: String,
)
