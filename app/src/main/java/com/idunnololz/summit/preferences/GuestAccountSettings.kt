package com.idunnololz.summit.preferences

import com.idunnololz.summit.api.LemmyApiClient.Companion.DEFAULT_INSTANCE
import kotlinx.serialization.Serializable

@Serializable
data class GuestAccountSettings(
    val instance: String = DEFAULT_INSTANCE,
)
