package com.idunnololz.summit.api.dto

data class SavePost(
    val post_id: PostId,
    val save: Boolean,
    val auth: String,
)
