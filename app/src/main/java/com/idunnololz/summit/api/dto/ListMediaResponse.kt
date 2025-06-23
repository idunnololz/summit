package com.idunnololz.summit.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ListMediaResponse(
    val images: List<LocalImageView>,
)
