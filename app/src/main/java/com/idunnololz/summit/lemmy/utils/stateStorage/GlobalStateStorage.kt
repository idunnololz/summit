package com.idunnololz.summit.lemmy.utils.stateStorage

import android.content.SharedPreferences
import com.idunnololz.summit.preferences.SharedPreferencesPreferences
import com.idunnololz.summit.preferences.floatPreference
import com.idunnololz.summit.preferences.stringPreference
import kotlinx.serialization.json.Json

class GlobalStateStorage(
  override val sharedPreferences: SharedPreferences,
  override val json: Json,
): SharedPreferencesPreferences {
  var videoStateVolume: Float
    by floatPreference("VIDEO_STATE_VOLUME")

  var colorPickerHistory: String?
    by stringPreference(
      "COLOR_PICKER_HISTORY",
      "#ffff4500,#ff7193ff,#FFF44336,#FFFFC107,#FFFF9800,#FF4CAF50,#FF2196F3,#FF607D8B"
    )
}
