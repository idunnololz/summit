package com.idunnololz.summit.api.dto

data class InstanceBlockView(
    val person: Person,
    val instance: Instance,
    val site: Site? = null,
)
