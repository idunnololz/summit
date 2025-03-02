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

    var colorPickerHistory: String?
        get() = prefs.getString(
            "COLOR_PICKER_HISTORY",
            "#ffff4500,#ff7193ff,#FFF44336,#FFFFC107,#FFFF9800,#FF4CAF50,#FF2196F3,#FF607D8B"
        )
        set(value) {
            prefs.edit()
                .putString("COLOR_PICKER_HISTORY", value)
                .apply()
        }
}
