package com.idunnololz.summit.api.dto

data class PersonView(
    val person: Person,
    val counts: PersonAggregates,
)
