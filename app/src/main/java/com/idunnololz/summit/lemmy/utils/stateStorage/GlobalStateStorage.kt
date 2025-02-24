package com.idunnololz.summit.lemmy.utils.stateStorage

import android.content.SharedPreferences

class GlobalStateStorage(
    private val prefs: SharedPreferences,
) {
    var videoStateVolume: Float
        get() = prefs.getFloat("VIDEO_STATE_VOLUME", 0f)
        set(value) {
            prefs.edit()
                .putFloat("VIDEO_STATE_VOLUME", value)
                .apply()
        }
}
