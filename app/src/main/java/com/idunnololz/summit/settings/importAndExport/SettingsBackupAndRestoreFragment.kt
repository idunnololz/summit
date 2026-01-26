package com.idunnololz.summit.settings.importAndExport

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.launchAlertDialog
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.ImportAndExportSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.importAndExport.ExportSettingsViewModel.BackupOption.SaveInternal
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.navigateSafe
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      exportSettingsViewModel.backupFile.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            loadingView.hideAll()
            launchAlertDialog("error_generating_backup") {
              messageResId = R.string.error_generating_backup
            }
          }
          is StatefulData.Loading -> {
            loadingView.showProgressBar()
          }
          is StatefulData.NotStarted -> {}
          is StatefulData.Success -> {
            loadingView.hideAll()

            when (it.data.config.backupOption) {
              ExportSettingsViewModel.BackupOption.Share -> {
                if (!isAdded) return@observe

                val sendIntent = Intent().apply {
                  putExtra(
                    Intent.EXTRA_STREAM,
                    it.data.uri,
                  )
                  action = Intent.ACTION_SEND
                  type = "application/lol-catalyst-backup"
                }

                startActivity(
                  Intent.createChooser(
                    sendIntent,
                    getString(R.string.share_backup),
                  ),
                )
              }
              ExportSettingsViewModel.BackupOption.Save -> {
                Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_LONG)
                  .show()
              }

              ExportSettingsViewModel.BackupOption.Copy -> {
                lifecycleScope.launch {
                  val text = context.contentResolver.openInputStream(it.data.uri)
                    ?.use {
                      it.bufferedReader().readText()
                    }

                  if (text != null) {
                    withContext(Dispatchers.Main) {
                      Utils.copyToClipboard(context, text)
                    }
                  } else {
                    withContext(Dispatchers.Main) {
                      launchAlertDialog("error_generating_backup") {
                        messageResId = R.string.error_generating_backup
                      }
                    }
                  }
                }
              }

              SaveInternal -> {
                Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_LONG)
                  .show()
              }
            }
          }
        }
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

    SettingModelItem.DividerItem(R.id.divider0),
    settings.copySettingsForDebugging.asCustomItem {
      exportSettingsViewModel.createBackupAndSave(
        ExportSettingsViewModel.BackupConfig(
          backupOption =  ExportSettingsViewModel.BackupOption.Copy,
          includeDatabase = false,
          dest = null,
        ),
      )
    },
  )
}
