package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class PersonView(
    val person: Person,
    val counts: PersonAggregates,
    val is_admin: Boolean,
)
