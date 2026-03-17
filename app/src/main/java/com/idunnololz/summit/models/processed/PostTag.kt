package com.idunnololz.summit.models.processed

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
sealed interface PostTag : Parcelable {

  @Serializable
  @Parcelize
  data object Spoiler : PostTag

  @Serializable
  @Parcelize
  data class CustomTag(val key: String) : PostTag
}