package com.idunnololz.summit.api.dto

data class LockPost(
    val post_id: PostId,
    val locked: Boolean,
    val auth: String,
)
