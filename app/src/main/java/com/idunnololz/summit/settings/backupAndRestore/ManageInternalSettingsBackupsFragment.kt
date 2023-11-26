package com.idunnololz.summit.settings.backupAndRestore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.BackupItemBinding
import com.idunnololz.summit.databinding.DialogFragmentManageInternalSettingsBackupsBinding
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManageInternalSettingsBackupsFragment :
    BaseFragment<DialogFragmentManageInternalSettingsBackupsBinding>() {

    private val viewModel: ManageInternalSettingsBackupsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(
            DialogFragmentManageInternalSettingsBackupsBinding.inflate(
                inflater,
                container,
                false,
            ),
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.manage_internal_settings_backups)
        }

        with(binding) {
            val adapter = BackupsAdapter(
                onBackupClick = { backupInfo ->
                    val bottomMenu = BottomMenu(context).apply {
                        addItemWithIcon(
                            id = R.id.delete,
                            title = R.string.delete_backup,
                            icon = R.drawable.baseline_delete_24,
                        )

                        setOnMenuItemClickListener {
                            when (it.id) {
                                R.id.delete -> {
                                    viewModel.deleteBackup(backupInfo)
                                }
                            }
                        }
                    }

                    getMainActivity()?.showBottomMenu(bottomMenu)
                },
            )

            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

            viewModel.backupsInfo.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.showDefaultErrorMessageFor(it.error)
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {
                        loadingView.hideAll()
                    }
                    is StatefulData.Success -> {
                        if (it.data.isEmpty()) {
                            loadingView.showErrorText(R.string.there_doesnt_seem_to_be_anything_here)
                        } else {
                            loadingView.hideAll()
                            adapter.backupData = it.data
                        }
                    }
                }
            }
        }
    }

    private class BackupsAdapter(
        private val onBackupClick: (SettingsBackupManager.BackupInfo) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

        var backupData: List<SettingsBackupManager.BackupInfo> = listOf()
            set(value) {
                field = value

                updateItems()
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int =
            adapterHelper.itemCount

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)
    }
}
