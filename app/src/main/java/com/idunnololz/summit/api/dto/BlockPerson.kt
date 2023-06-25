package com.idunnololz.summit.api.dto

data class BlockPerson(
    val person_id: PersonId,
    val block: Boolean,
    val auth: String,
)
