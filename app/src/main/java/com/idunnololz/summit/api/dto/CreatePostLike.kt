package com.idunnololz.summit.api.dto

data class CreatePostLike(
    val post_id: PostId,
    val score: Int,
    val auth: String,
)
