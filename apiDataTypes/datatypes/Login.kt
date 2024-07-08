package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class Login(
    val username_or_email: String,
    val password: String,
    val totp_2fa_token: String? = null,
)
