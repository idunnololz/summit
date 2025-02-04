package com.idunnololz.summit.settings.backupAndRestore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.DialogFragmentBackupSettingsBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ExportSettingsDialogFragment :
    BaseDialogFragment<DialogFragmentBackupSettingsBinding>(),
    FullscreenDialogFragment {

    companion object {

        fun show(fragmentManager: FragmentManager) {
            ExportSettingsDialogFragment()
                .showAllowingStateLoss(fragmentManager, "BackupSettingsDialogFragment")
        }
    }

    private val viewModel: ExportSettingsViewModel by viewModels()

    private val chooseSaveLocationLauncher =
        registerForActivityResult(
            object : ActivityResultContracts.CreateDocument("application/lol-catalyst-backup") {
                override fun createIntent(context: Context, input: String): Intent {
                    return super.createIntent(context, input).apply {
                        type = "application/lol-catalyst-backup"
                    }
                }
            },
        ) { uri ->
            if (uri != null) {
                viewModel.createBackupAndSave(
                    ExportSettingsViewModel.BackupConfig(
                        backupOption = ExportSettingsViewModel.BackupOption.Save,
                        dest = uri,
                    ),
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentBackupSettingsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)
        }

        with(binding) {
            viewModel.backupFile.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        progressBar.visibility = View.GONE
                        OldAlertDialogFragment.Builder()
                            .setMessage(R.string.error_generating_backup)
                            .createAndShow(childFragmentManager, "error_generating_backup")
                    }
                    is StatefulData.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        progressBar.visibility = View.GONE

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
                                dismiss()

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
                                            OldAlertDialogFragment.Builder()
                                                .setMessage(R.string.error_generating_backup)
                                                .createAndShow(
                                                    childFragmentManager,
                                                    "error_generating_backup",
                                                )
                                        }
                                    }
                                }
                            }

                            ExportSettingsViewModel.BackupOption.SaveInternal -> {
                                dismiss()

                                Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    }
                }
            }

            toolbar.title = getString(R.string.backup_settings)
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            share.setOnClickListener {
                viewModel.createBackupAndSave(
                    ExportSettingsViewModel.BackupConfig(
                        ExportSettingsViewModel.BackupOption.Share,
                    ),
                )
            }
            save.setOnClickListener {
                chooseSaveLocationLauncher.launch(viewModel.getBackupFileName())
            }
            copyToClipboard.setOnClickListener {
                viewModel.createBackupAndSave(
                    ExportSettingsViewModel.BackupConfig(
                        ExportSettingsViewModel.BackupOption.Copy,
                    ),
                )
            }
            saveToInternalBackups.setOnClickListener {
                viewModel.saveToInternalBackups()
            }
        }
    }
}
