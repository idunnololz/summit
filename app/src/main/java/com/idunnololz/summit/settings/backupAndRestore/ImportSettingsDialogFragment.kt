package com.idunnololz.summit.settings.backupAndRestore

import android.app.Dialog
import android.graphics.Paint
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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.transition.TransitionManager
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.BackupItemBinding
import com.idunnololz.summit.databinding.DialogFragmentImportSettingsBinding
import com.idunnololz.summit.databinding.ImportSettingItemBinding
import com.idunnololz.summit.databinding.ImportSettingsFromBackupsBinding
import com.idunnololz.summit.settings.backupAndRestore.ImportSettingsViewModel.SettingsDataPreview
import com.idunnololz.summit.util.BackPressHandler
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ImportSettingsDialogFragment :
    BaseDialogFragment<DialogFragmentImportSettingsBinding>(),
    FullscreenDialogFragment,
    BackPressHandler,
    AlertDialogFragment.AlertDialogFragmentListener {

    companion object {

        fun show(
            fragmentManager: FragmentManager,
        ) {
            ImportSettingsDialogFragment()
                .showAllowingStateLoss(fragmentManager, "ImportSettingsDialogFragment")
        }
    }

    private val viewModel: ImportSettingsViewModel by viewModels()

    @Inject
    lateinit var settingsBackupManager: SettingsBackupManager

    private val chooseFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                viewModel.generatePreviewFromFile(uri)
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
                onBackPressed()
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

                        AlertDialogFragment.Builder()
                            .setMessage(errorMessage)
                            .createAndShow(childFragmentManager, "error_dialog")

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
                        AlertDialogFragment.Builder()
                            .setTitle(R.string.app_restart_required)
                            .setMessage(R.string.app_restart_required_desc)
                            .setPositiveButton(R.string.restart_app)
                            .setNegativeButton(R.string.restart_later)
                            .createAndShow(childFragmentManager, "restart_required")
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
                data,
                onSettingPreviewClick = { settingKey, settingsDataPreview ->
                    ImportSettingItemPreviewDialogFragment.show(
                        childFragmentManager,
                        settingKey,
                        settingsDataPreview.settingsPreview[settingKey] ?: "",
                        settingsDataPreview.keyToType[settingKey] ?: "?",
                    )
                },
            )

            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

            confirmImport.setOnClickListener {
                viewModel.confirmImport(adapter.excludeKeys)
            }
        }
    }

    override fun onBackPressed(): Boolean {
        try {
            dismiss()
        } catch (e: IllegalStateException) {
            // do nothing... very rare
        }
        return true
    }

    private class SettingDataAdapter(
        private val settingsDataPreview: SettingsDataPreview,
        private val onSettingPreviewClick: (settingKey: String, settingsDataPreview: SettingsDataPreview) -> Unit,
    ) : RecyclerView.Adapter<ViewHolder>() {

        private sealed interface Item {
            /**
             * A single setting within the imported setting data.
             */
            data class ImportSettingItem(
                val settingKey: String,
                val value: Any,
                val isExcluded: Boolean,
            ) : Item
        }

        var excludeKeys = mutableSetOf<String>()

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { oldItem, newItem ->
                oldItem::class == newItem::class && when (oldItem) {
                    is Item.ImportSettingItem ->
                        oldItem.settingKey == (newItem as Item.ImportSettingItem).settingKey
                }
            },
        ).apply {
            addItemType(
                clazz = Item.ImportSettingItem::class,
                inflateFn = ImportSettingItemBinding::inflate,
            ) { item, b, h ->
                b.settingKey.text = item.settingKey.lowercase()
                b.settingValue.text = item.value.toString()
                b.root.setOnClickListener {
                    onSettingPreviewClick(item.settingKey, settingsDataPreview)
                }

                if (item.isExcluded) {
                    b.settingKey.apply {
                        paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        alpha = 0.5f
                        invalidate()
                    }
                    b.settingValue.apply {
                        paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        alpha = 0.5f
                        invalidate()
                    }

                    b.remove.setImageResource(R.drawable.baseline_add_24)
                    b.remove.setOnClickListener {
                        excludeKeys.remove(item.settingKey)
                        updateItems()
                    }
                } else {
                    b.settingKey.apply {
                        paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        alpha = 1f
                        invalidate()
                    }
                    b.settingValue.apply {
                        paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        alpha = 1f
                        invalidate()
                    }
                    b.remove.setImageResource(R.drawable.baseline_close_24)
                    b.remove.setOnClickListener {
                        excludeKeys.add(item.settingKey)
                        updateItems()
                    }
                }
            }
        }

        init {
            updateItems()
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        override fun getItemCount(): Int = adapterHelper.itemCount

        private fun updateItems() {
            val newItems = mutableListOf<Item>()
            val newData = settingsDataPreview

            newData.settingsPreview.mapTo(newItems) {
                Item.ImportSettingItem(
                    settingKey = it.key,
                    value = it.value,
                    isExcluded = excludeKeys.contains(it.key),
                )
            }

            adapterHelper.setItems(newItems, this)
        }
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            "restart_required" -> {
                ProcessPhoenix.triggerRebirth(requireContext())
            }
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            "restart_required" -> {
                dismiss()
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

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int =
            adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)
    }
}
