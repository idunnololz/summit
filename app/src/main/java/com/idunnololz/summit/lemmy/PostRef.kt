package com.idunnololz.summit.lemmy

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class PostRef(
    val instance: String,
    val id: Int,
) : PageRef, Parcelable
