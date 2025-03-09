package com.idunnololz.summit.preferences

import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ProviderInfo
import android.content.pm.ServiceInfo
import kotlinx.serialization.Serializable

@Serializable
data class DefaultAppPreference(
    var appName: String,
    var packageName: String,
)