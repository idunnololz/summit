package com.idunnololz.summit.preferences

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class PostHeaderVersion(
  val value: Int,
) {
  companion object {
    val V1 = PostHeaderVersion(0)
    val V2 = PostHeaderVersion(1)
  }
}
