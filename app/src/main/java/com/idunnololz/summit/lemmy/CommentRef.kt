package com.idunnololz.summit.lemmy

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CommentRef(
    val instance: String,
    val id: Int,
) : PageRef