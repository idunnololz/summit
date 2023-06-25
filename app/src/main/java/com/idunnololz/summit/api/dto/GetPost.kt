package com.idunnololz.summit.api.dto

data class GetPost(
    val id: PostId? = null,
    val comment_id: CommentId? = null,
    val auth: String? = null,
)
