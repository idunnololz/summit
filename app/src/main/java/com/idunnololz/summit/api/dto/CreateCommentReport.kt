package com.idunnololz.summit.api.dto

data class CreateCommentReport(
    val comment_id: CommentId,
    val reason: String,
    val auth: String,
)
