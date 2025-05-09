package com.idunnololz.summit.settings.importAndExport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingsBackupAndRestoreBinding
import com.idunnololz.summit.settings.ImportAndExportSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsBackupAndRestoreFragment :
  BaseFragment<FragmentSettingsBackupAndRestoreBinding>(),
  OldAlertDialogFragment.AlertDialogFragmentListener {

  @Inject
  lateinit var settings: ImportAndExportSettings

  private val exportSettingsViewModel: ExportSettingsViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentSettingsBackupAndRestoreBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    requireSummitActivity().apply {
      setupForFragment<SettingsFragment>()
      insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
      insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

      setSupportActionBar(binding.toolbar)

      supportActionBar?.setDisplayShowHomeEnabled(true)
      supportActionBar?.setDisplayHomeAsUpEnabled(true)
      supportActionBar?.title = settings.getPageName(context)
    }

    updateRendering()
  }

  private fun updateRendering() {
    with(binding) {
      settings.exportSettings.bindTo(
        backupSettings,
      ) {
        ExportSettingsDialogFragment.show(childFragmentManager)
      }

      settings.importSettings.bindTo(
        restoreSettings,
      ) {
        ImportSettingsDialogFragment.show(childFragmentManager)
      }

      settings.resetSettingsWithBackup.bindTo(
        resetSettingsWithBackup,
      ) {
        OldAlertDialogFragment.Builder()
          .setTitle(R.string.reset_settings_prompt)
          .setMessage(R.string.backup_and_reset_settings_desc)
          .setPositiveButton(R.string.reset_settings)
          .setNegativeButton(R.string.cancel)
          .createAndShow(childFragmentManager, "reset_settings")
      }

      settings.manageInternalSettingsBackups.bindTo(
        manageInternalSettingsBackups,
      ) {
        val direction = SettingsBackupAndRestoreFragmentDirections
          .actionSettingBackupAndRestoreFragmentToManageInternalSettingsBackupsDialogFragment()
        findNavController().navigateSafe(direction)
      }

      settings.viewCurrentSettings.bindTo(
        viewCurrentSettings,
      ) {
        val direction = SettingsBackupAndRestoreFragmentDirections
          .actionSettingBackupAndRestoreFragmentToViewCurrentSettingsFragment()
        findNavController().navigateSafe(direction)
      }
    }
  }

  override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
    when (tag) {
      "reset_settings" -> {
        exportSettingsViewModel.saveToInternalBackups(
          backupName = "reset_settings_backup_%datetime%",
          backupConfig = ExportSettingsViewModel.BackupConfig(
            backupOption = ExportSettingsViewModel.BackupOption.SaveInternal,
            includeDatabase = true,
          ),
        )
        exportSettingsViewModel.resetSettings()

        OldAlertDialogFragment.Builder()
          .setTitle(R.string.app_restart_required)
          .setMessage(R.string.app_restart_required_after_pref_cleared_desc)
          .setPositiveButton(R.string.restart_app)
          .setNegativeButton(R.string.restart_later)
          .createAndShow(childFragmentManager, "restart_required")
      }
      "restart_required" -> {
        ProcessPhoenix.triggerRebirth(requireContext())
      }
    }
  }

  override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    when (tag) {
      "restart_required" -> {
        // do nothing
      }
    }
  }
}
