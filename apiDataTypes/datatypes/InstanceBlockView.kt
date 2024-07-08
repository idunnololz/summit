package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class InstanceBlockView(
    val person: Person,
    val instance: Instance,
    val site: Site? = null,
)
