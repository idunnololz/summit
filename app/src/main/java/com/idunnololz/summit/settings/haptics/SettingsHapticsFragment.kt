package com.idunnololz.summit.settings.haptics

import android.content.Intent
import android.provider.Settings
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.HapticSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asOnOffMasterSwitch
import com.idunnololz.summit.settings.util.asOnOffSwitch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsHapticsFragment : BaseSettingsFragment() {

  @Inject
  override lateinit var settings: HapticSettings

  private val warnHapticsDisabledDialogLauncher = newAlertDialogLauncher("warn_haptics_disabled") {
    if (it.isOk) {
      val intent = Intent(Settings.ACTION_SOUND_SETTINGS)
      startActivity(intent)
    }
  }

  override fun generateData(): List<SettingModelItem> {
    val context = requireContext()

    return listOf(
      settings.haptics.asOnOffMasterSwitch(
        { preferences.hapticsEnabled },
        {
          preferences.hapticsEnabled = it

          if (it) {
            val hapticsEnabled =
              Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1,
              ) ==
                1

            if (!hapticsEnabled) {
              warnHapticsDisabledDialogLauncher.launchDialog {
                messageResId = R.string.warn_system_haptic_feedback_disabled
                positionButtonResId = R.string.settings
                negativeButtonResId = R.string.cancel
              }
            }
          }
        },
      ),
      settings.moreHaptics.asOnOffSwitch(
        { preferences.hapticsOnActions },
        {
          preferences.hapticsOnActions = it
        },
      ),
    )
  }
}
