package com.idunnololz.summit.api.dto

data class GetComment(
    val id: CommentId,
    val auth: String? = null,
)
