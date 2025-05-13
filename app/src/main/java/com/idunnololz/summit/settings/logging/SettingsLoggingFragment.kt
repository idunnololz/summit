package com.idunnololz.summit.settings.logging

import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.LoggingSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsLoggingFragment :
  BaseSettingsFragment() {

  @Inject
  override lateinit var settings: LoggingSettings

  private val enableLogCrashesDialogLauncher = newAlertDialogLauncher("enable_log_crashes") {
    if (it.isOk) {
      preferences.useFirebase = true
      ProcessPhoenix.triggerRebirth(requireContext())
    } else {
      refresh()
    }
  }
  private val disableLogCrashesDialogLauncher = newAlertDialogLauncher("disable_log_crashes") {
    if (it.isOk) {
      preferences.useFirebase = false
      ProcessPhoenix.triggerRebirth(requireContext())
    } else {
      refresh()
    }
  }

  override fun generateData(): List<SettingModelItem> {
    return listOf(
      settings.useFirebase.asOnOffSwitch(
        { preferences.useFirebase },
        {
          if (it) {
            enableLogCrashesDialogLauncher
          } else {
            disableLogCrashesDialogLauncher
          }.launchDialog {
            titleResId = R.string.app_restart_required_by_setting
            messageResId = R.string.app_restart_required_by_setting_desc
            positionButtonResId = R.string.restart_app
            negativeButtonResId = R.string.cancel
          }
        },
      ),
    )
  }
}
