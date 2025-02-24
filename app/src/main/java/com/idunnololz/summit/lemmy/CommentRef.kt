package com.idunnololz.summit.lemmy

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class CommentRef(
    val instance: String,
    val id: Int,
) : PageRef, Parcelable
