package com.idunnololz.summit.lemmy

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class CommentRef(
    val instance: String,
    val id: Int,
) : PageRef, Parcelable
