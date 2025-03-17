package com.idunnololz.summit.preferences

import kotlinx.serialization.Serializable

@Serializable
data class DefaultAppPreference(
    val appName: String,
    val packageName: String,
    val componentName: String? = null,
)
