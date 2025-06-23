package com.idunnololz.summit.api.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ListMedia(
    val page: Long? = null,
    val limit: Long? = null,
)
