package com.idunnololz.summit.preferences

import com.idunnololz.summit.lemmy.Consts.DEFAULT_INSTANCE
import kotlinx.serialization.Serializable

@Serializable
data class GuestAccountSettings(
  val instance: String = DEFAULT_INSTANCE,
)
