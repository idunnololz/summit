package com.idunnololz.summit.settings.videoPlayer

import com.idunnololz.summit.R
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.VideoPlayerSettings
import com.idunnololz.summit.settings.util.asOnOffSwitch
import com.idunnololz.summit.settings.util.asSliderItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsVideoPlayerFragment :
  BaseSettingsFragment() {

  @Inject
  override lateinit var settings: VideoPlayerSettings

  override fun generateData(): List<SettingModelItem> = listOf(
    settings.autoHideUiOnPlay.asOnOffSwitch(
      { preferences.autoHideUiOnPlay },
      {
        preferences.autoHideUiOnPlay = it
      },
    ),
    settings.tapAnywhereToPlayPause.asOnOffSwitch(
      { preferences.tapAnywhereToPlayPause },
      {
        preferences.tapAnywhereToPlayPause = it
      },
    ),
    settings.loopVideoByDefault.asOnOffSwitch(
      { preferences.loopVideoByDefault },
      {
        preferences.loopVideoByDefault = it
      },
    ),
    SettingModelItem.SubgroupItem(
      getString(R.string.inline_video_player),
      listOf(
        settings.autoPlayVideos.asOnOffSwitch(
          { preferences.autoPlayVideos },
          {
            preferences.autoPlayVideos = it
          },
        ),
        settings.inlineVideoVolume.asSliderItem(
          { preferences.inlineVideoDefaultVolume },
          { preferences.inlineVideoDefaultVolume = it },
        ),
      ),
    ),
  )
}
