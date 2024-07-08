package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class Language(
    val id: LanguageId,
    val code: String,
    val name: String,
)
