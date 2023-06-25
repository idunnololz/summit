package com.idunnololz.summit.api.dto

data class PurgePost(
    val post_id: PostId,
    val reason: String? = null,
    val auth: String,
)
