package com.idunnololz.summit.settings.backupAndRestore

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.transition.TransitionManager
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.launchAlertDialog
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.databinding.BackupItemBinding
import com.idunnololz.summit.databinding.DialogFragmentImportSettingsBinding
import com.idunnololz.summit.databinding.ImportSettingsFromBackupsBinding
import com.idunnololz.summit.db.MainDatabase.Companion.DATABASE_NAME
import com.idunnololz.summit.db.preview.TableDetailsDialogFragment
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ImportSettingsDialogFragment :
    BaseDialogFragment<DialogFragmentImportSettingsBinding>(),
    FullscreenDialogFragment {

    companion object {

        fun show(fragmentManager: FragmentManager) {
            ImportSettingsDialogFragment()
                .showAllowingStateLoss(fragmentManager, "ImportSettingsDialogFragment")
        }
    }

    private val viewModel: ImportSettingsViewModel by viewModels()

    @Inject
    lateinit var settingsBackupManager: SettingsBackupManager

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    private val chooseFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                viewModel.generatePreviewFromFile(uri)
            }
        }

    private val restartAppDialogLauncher = newAlertDialogLauncher("restart_required") {
        if (it.isOk) {
            ProcessPhoenix.triggerRebirth(requireContext())
        } else {
            dismiss()
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

        setBinding(DialogFragmentImportSettingsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)
        }

        with(binding) {
            toolbar.title = getString(R.string.import_settings)
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            importFromCode.setOnClickListener {
                val text = importSettingsEditText.text
                if (text.isNullOrBlank()) {
                    importSettingsTextInput.error = getString(R.string.settings_code_missing)
                } else {
                    importSettingsTextInput.error = null
                    viewModel.importSettings(text.toString())
                }
            }
            importFromFile.setOnClickListener {
                chooseFileLauncher.launch("*/*")
            }
            importSettingsTextInput.setEndIconOnClickListener {
                importSettingsEditText.setText(Utils.getFromClipboard(context))
            }
            importFromInternalBackup.setOnClickListener {
                onImportFromInternalBackup()
            }

            viewModel.state.observe(viewLifecycleOwner) {
                loadingView.hideAll()

                when (it) {
                    is ImportSettingsViewModel.State.ConfirmImportSettings -> {
                        showRecyclerViewWithData(it.preview)
                    }
                    is ImportSettingsViewModel.State.Error -> {
                        val errorMessage: String = when (it.error) {
                            ImportSettingsViewModel.ErrorType.UnableToDecipherInput -> {
                                getString(R.string.error_unable_to_decipher_input)
                            }

                            ImportSettingsViewModel.ErrorType.InvalidSettingsJson -> {
                                getString(R.string.error_input_is_not_settings_json)
                            }

                            ImportSettingsViewModel.ErrorType.InvalidJson -> {
                                getString(R.string.error_input_is_not_json)
                            }
                        }

                        launchAlertDialog("error_dialog") {
                            message = errorMessage
                        }

                        viewModel.state.postValue(ImportSettingsViewModel.State.NotStarted)
                    }
                    is ImportSettingsViewModel.State.DecodeInputString,
                    is ImportSettingsViewModel.State.GeneratePreviewFromSettingsJson,
                    is ImportSettingsViewModel.State.PerformImportSettings,
                    -> {
                        loadingView.showProgressBar()
                    }
                    ImportSettingsViewModel.State.NotStarted -> {}
                    ImportSettingsViewModel.State.ImportSettingsCompleted -> {
                        restartAppDialogLauncher.launchDialog {
                            titleResId = R.string.app_restart_required
                            messageResId = R.string.app_restart_required_desc
                            positionButtonResId = R.string.restart_app
                            negativeButtonResId = R.string.restart_later
                        }
                    }
                }
            }
        }
    }

    private fun onImportFromInternalBackup() {
        val context = requireContext()

        val backups = settingsBackupManager.getBackups()
        val b = ImportSettingsFromBackupsBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context, R.style.Theme_App_Dialog)

        with(b) {
            recyclerView.apply {
                setup(animationsHelper)
                adapter = BackupsAdapter(
                    backups,
                    onBackupClick = {
                        viewModel.generatePreviewFromFile(it.file.toUri())
                        dialog.dismiss()
                    },
                )
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
            }

            if (backups.isEmpty()) {
                loadingView.showErrorText(R.string.no_backups)
            } else {
                loadingView.hideAll()
            }

            toolbar.title = getString(R.string.choose_a_backup)
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.apply {
            setContentView(b.root)
            show()

            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT

            val window = checkNotNull(window)
            window.setLayout(width, height)
        }
    }

    private fun showRecyclerViewWithData(data: SettingsDataPreview) {
        with(binding) {
            TransitionManager.beginDelayedTransition(root)

            importSettingsView.visibility = View.GONE
            confirmImportView.visibility = View.VISIBLE

            val adapter = SettingDataAdapter(
                context = binding.root.context,
                isImporting = true,
                onSettingPreviewClick = { settingKey, settingsDataPreview ->
                    ImportSettingItemPreviewDialogFragment.show(
                        childFragmentManager,
                        settingKey,
                        settingsDataPreview.settingsPreview[settingKey] ?: "",
                        settingsDataPreview.keyToType[settingKey] ?: "?",
                    )
                },
                onTableClick = {
                    TableDetailsDialogFragment.show(
                        childFragmentManager,
                        requireContext().getDatabasePath(DATABASE_NAME).toUri(),
                        it,
                    )
                },
                onDeleteClick = {
                    excludeKeys.add(it)
                },
            ).apply {
                setData(data)
            }

            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

            confirmImport.setOnClickListener {
                viewModel.confirmImport(adapter.excludeKeys, adapter.tableResolutions)
            }
        }
    }

    private class BackupsAdapter(
        private val backupData: List<SettingsBackupManager.BackupInfo>,
        private val onBackupClick: (SettingsBackupManager.BackupInfo) -> Unit,
    ) : Adapter<ViewHolder>() {

        private sealed interface Item {
            data class BackupItem(
                val backupInfo: SettingsBackupManager.BackupInfo,
            ) : Item
        }

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { oldItem, newItem ->
                oldItem::class == newItem::class && when (oldItem) {
                    is Item.BackupItem ->
                        oldItem.backupInfo.file.path ==
                            (newItem as Item.BackupItem).backupInfo.file.path
                }
            },
        ).apply {
            addItemType(Item.BackupItem::class, BackupItemBinding::inflate) { item, b, h ->
                b.text.text = item.backupInfo.file.name
                b.root.setOnClickListener {
                    onBackupClick(item.backupInfo)
                }
            }
        }

        init {
            updateItems()
        }

        private fun updateItems() {
            val newItems = backupData.map { Item.BackupItem(it) }

            adapterHelper.setItems(newItems, this)
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)
    }
}
