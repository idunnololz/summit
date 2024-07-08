package com.idunnololz.summit.api.dto

data class MarkPostAsRead(
    val post_ids: List<PostId>? = null,
    val post_id: PostId,
    val read: Boolean,
    val auth: String,
)
