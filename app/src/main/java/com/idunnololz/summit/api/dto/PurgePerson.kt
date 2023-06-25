package com.idunnololz.summit.api.dto

data class PurgePerson(
    val person_id: PersonId,
    val reason: String? = null,
    val auth: String,
)
