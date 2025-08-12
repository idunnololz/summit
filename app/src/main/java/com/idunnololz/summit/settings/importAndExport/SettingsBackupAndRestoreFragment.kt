package com.idunnololz.summit.settings.importAndExport

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.ImportAndExportSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.util.ext.navigateSafe
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsBackupAndRestoreFragment : BaseSettingsFragment() {

  @Inject
  override lateinit var settings: ImportAndExportSettings

  private val exportSettingsViewModel: ExportSettingsViewModel by viewModels()

  private val restartAppDialogLauncher = newAlertDialogLauncher("restart") {
    if (it.isOk) {
      ProcessPhoenix.triggerRebirth(requireContext())
    }
  }

  private val resetSettingsDialogLauncher = newAlertDialogLauncher("reset_settings") {
    if (it.isOk) {
      exportSettingsViewModel.saveToInternalBackups(
        backupName = "reset_settings_backup_%datetime%",
        backupConfig = ExportSettingsViewModel.BackupConfig(
          backupOption = ExportSettingsViewModel.BackupOption.SaveInternal,
          includeDatabase = true,
        ),
      )
      exportSettingsViewModel.resetSettings()

      restartAppDialogLauncher.launchDialog {
        titleResId = R.string.app_restart_required
        messageResId = R.string.app_restart_required_after_pref_cleared_desc
        positionButtonResId = R.string.restart_app
        negativeButtonResId = R.string.restart_later
      }
    }
  }

  override fun generateData(): List<SettingModelItem> = listOf(
    settings.importSettings.asCustomItem {
      ImportSettingsDialogFragment.show(childFragmentManager)
    },
    settings.exportSettings.asCustomItem {
      ExportSettingsDialogFragment.show(childFragmentManager)
    },
    settings.resetSettingsWithBackup.asCustomItem {
      resetSettingsDialogLauncher.launchDialog {
        titleResId = R.string.reset_settings_prompt
        messageResId = R.string.backup_and_reset_settings_desc
        positionButtonResId = R.string.reset_settings
        negativeButtonResId = R.string.cancel
      }
    },
    settings.manageInternalSettingsBackups.asCustomItem {
      val direction = SettingsBackupAndRestoreFragmentDirections
        .actionSettingBackupAndRestoreFragmentToManageInternalSettingsBackupsDialogFragment()
      findNavController().navigateSafe(direction)
    },
    settings.viewCurrentSettings.asCustomItem {
      val direction = SettingsBackupAndRestoreFragmentDirections
        .actionSettingBackupAndRestoreFragmentToViewCurrentSettingsFragment()
      findNavController().navigateSafe(direction)
    },
  )
}
