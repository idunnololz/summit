package com.idunnololz.summit.api.dto

data class GetPostsResponse(
    val posts: List<PostView>,
    val next_page: String?,
)
