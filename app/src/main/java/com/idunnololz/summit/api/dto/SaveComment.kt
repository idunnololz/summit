package com.idunnololz.summit.api.dto

data class SaveComment(
    val comment_id: CommentId,
    val save: Boolean,
    val auth: String,
)
