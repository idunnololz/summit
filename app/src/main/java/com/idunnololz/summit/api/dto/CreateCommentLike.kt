package com.idunnololz.summit.api.dto

data class CreateCommentLike(
    val comment_id: CommentId,
    val score: Int,
    val auth: String,
)
