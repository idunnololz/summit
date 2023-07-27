package com.idunnololz.summit.lemmy

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class PostRef(
    val instance: String,
    val id: Int,
) : PageRef, Parcelable
