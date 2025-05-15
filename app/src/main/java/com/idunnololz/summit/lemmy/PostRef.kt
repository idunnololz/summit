package com.idunnololz.summit.lemmy

import android.os.Parcelable
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.instance
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class PostRef(
  val instance: String,
  val id: Int,
) : PageRef, Parcelable