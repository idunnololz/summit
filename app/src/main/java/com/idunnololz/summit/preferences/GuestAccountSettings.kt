package com.idunnololz.summit.preferences

import com.idunnololz.summit.api.LemmyApiClient.Companion.DEFAULT_INSTANCE
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GuestAccountSettings(
    val instance: String = DEFAULT_INSTANCE
)