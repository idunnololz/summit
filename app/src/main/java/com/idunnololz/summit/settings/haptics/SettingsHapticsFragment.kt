package com.idunnololz.summit.settings.haptics

import android.content.Intent
import android.provider.Settings
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.HapticSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.asOnOffMasterSwitch
import com.idunnololz.summit.settings.asOnOffSwitch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsHapticsFragment :
  BaseSettingsFragment(),
  OldAlertDialogFragment.AlertDialogFragmentListener {

  @Inject
  override lateinit var settings: HapticSettings

  override fun generateData(): List<SettingModelItem> {
    val context = requireContext()

    return listOf(
      settings.haptics.asOnOffMasterSwitch(
        { preferences.hapticsEnabled },
        {
          preferences.hapticsEnabled = it

          if (it) {
            val hapticsEnabled =
              Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1

            if (!hapticsEnabled) {
              OldAlertDialogFragment.Builder()
                .setMessage(R.string.warn_system_haptic_feedback_disabled)
                .setPositiveButton(R.string.settings)
                .setNegativeButton(R.string.cancel)
                .createAndShow(childFragmentManager, "warn_haptics_disabled")
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

  override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
    val intent = Intent(Settings.ACTION_SOUND_SETTINGS)
    startActivity(intent)
  }

  override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
  }
}
