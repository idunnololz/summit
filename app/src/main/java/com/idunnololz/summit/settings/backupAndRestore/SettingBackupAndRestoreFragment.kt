package com.idunnololz.summit.settings.backupAndRestore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.databinding.FragmentSettingBackupAndRestoreBinding
import com.idunnololz.summit.settings.ImportAndExportSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingBackupAndRestoreFragment : BaseFragment<FragmentSettingBackupAndRestoreBinding>() {

    @Inject
    lateinit var settings: ImportAndExportSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingBackupAndRestoreBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
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
                backupSettings
            ) {
                ExportSettingsDialogFragment.show(childFragmentManager)
            }

            settings.importSettings.bindTo(
                restoreSettings
            ) {
                ImportSettingsDialogFragment.show(childFragmentManager)
            }

            settings.manageInternalSettingsBackups.bindTo(
                manageInternalSettingsBackups
            ) {
                val direction = SettingBackupAndRestoreFragmentDirections
                    .actionSettingBackupAndRestoreFragmentToManageInternalSettingsBackupsDialogFragment()
                findNavController().navigateSafe(direction)
            }
        }
    }
}