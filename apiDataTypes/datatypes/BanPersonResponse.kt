package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class BanPersonResponse(
    val person_view: PersonView,
    val banned: Boolean,
)
