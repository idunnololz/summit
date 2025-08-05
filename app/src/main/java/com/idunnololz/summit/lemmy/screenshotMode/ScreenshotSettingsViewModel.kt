package com.idunnololz.summit.lemmy.screenshotMode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.idunnololz.summit.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ScreenshotSettingsViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  val preferences: Preferences
) : ViewModel() {

  val screenshotWatermarkId =
    savedStateHandle.getLiveData("screenshot_watermark_id", preferences.screenshotWatermark)
}