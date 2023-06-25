package com.idunnololz.summit.api.dto

data class DeletePost(
    val post_id: PostId,
    val deleted: Boolean,
    val auth: String,
)
