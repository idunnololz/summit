package com.idunnololz.summit.lemmy

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostRef(
    val instance: String,
    val id: Int,
) : PageRef