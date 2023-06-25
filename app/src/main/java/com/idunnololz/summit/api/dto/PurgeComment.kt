package com.idunnololz.summit.api.dto

data class PurgeComment(
    val comment_id: CommentId,
    val reason: String? = null,
    val auth: String,
)
