package com.idunnololz.summit.settings.perAccount

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.EmptyItemBinding
import com.idunnololz.summit.databinding.FragmentManageSettingsBinding
import com.idunnololz.summit.databinding.ImportSettingItemBinding
import com.idunnololz.summit.databinding.ManageSettingsInstructionItemBinding
import com.idunnololz.summit.settings.AllSettings
import com.idunnololz.summit.settings.SettingItem
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ManageSettingsFragment :
    BaseFragment<FragmentManageSettingsBinding>(),
    OldAlertDialogFragment.AlertDialogFragmentListener {

    private val args: ManageSettingsFragmentArgs by navArgs()
    private val viewModel: ManageSettingsViewModel by viewModels()

    @Inject
    lateinit var allSettings: AllSettings

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentManageSettingsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.recyclerView)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = context.getString(R.string.manage_settings)
            supportActionBar?.subtitle = args.account?.fullName
        }

        viewModel.loadManageSettingsData(args.account)

        with(binding) {
            val adapter = ManageSettingsDataAdapter(
                context = context,
                keyToSettingItems = allSettings.generateMapFromKeysToRelatedSettingItems(),
                onClearSettingClick = { settingKey ->
                    if (args.account != null) {
                        OldAlertDialogFragment.Builder()
                            .setMessage(
                                getString(
                                    R.string.delete_setting_override_for_setting_format,
                                    settingKey,
                                ),
                            )
                            .setNegativeButton(R.string.cancel)
                            .setPositiveButton(R.string.delete)
                            .setExtra("setting_key", settingKey)
                            .createAndShow(childFragmentManager, "delete_confirm")
                    }
                },
            )

            viewModel.manageSettingsData.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.showDefaultErrorMessageFor(it.error)
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()

                        adapter.manageSettingsData = it.data
                    }
                }
            }

            recyclerView.setup(animationsHelper)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            recyclerView.setHasFixedSize(true)
        }
    }

    private class ManageSettingsDataAdapter(
        private val context: Context,
        private val keyToSettingItems: MutableMap<String, MutableList<SettingItem>>,
        private val onClearSettingClick: (key: String) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private sealed interface Item {

            data object InstructionItem : Item
            data object EmptyItem : Item

            /**
             * A single setting within the imported setting data.
             */
            data class SettingListItem(
                val settingKey: String,
                val value: Any,
                val relatedSettings: List<SettingItem>,
            ) : Item
        }

        var manageSettingsData: ManageSettingsViewModel.ManageSettingsData? = null
            set(value) {
                field = value

                updateItems()
            }

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { oldItem, newItem ->
                oldItem::class == newItem::class && when (oldItem) {
                    is Item.SettingListItem ->
                        oldItem.settingKey == (newItem as Item.SettingListItem).settingKey

                    Item.EmptyItem,
                    Item.InstructionItem,
                    -> true
                }
            },
        ).apply {
            addItemType(
                clazz = Item.SettingListItem::class,
                inflateFn = ImportSettingItemBinding::inflate,
            ) { item, b, h ->
                b.settingTitle.text = item.relatedSettings.firstOrNull()?.title
                    ?: context.getString(R.string.unknown_special)
                b.settingKey.text = item.settingKey.lowercase()
                b.settingValue.text = item.value.toString()

                b.remove.setImageResource(R.drawable.baseline_close_24)
                b.remove.setOnClickListener {
                    onClearSettingClick(item.settingKey)
                }
            }
            addItemType(
                clazz = Item.InstructionItem::class,
                inflateFn = ManageSettingsInstructionItemBinding::inflate,
            ) { item, b, h ->
                b.desc.text = context.getString(R.string.manage_settings_desc)
            }
            addItemType(
                clazz = Item.EmptyItem::class,
                inflateFn = EmptyItemBinding::inflate,
            ) { _, _, _ -> }
        }

        init {
            updateItems()
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        override fun getItemCount(): Int = adapterHelper.itemCount

        private fun updateItems() {
            val newItems = mutableListOf<Item>()
            val newData = manageSettingsData

            newItems.add(Item.InstructionItem)

            if (newData == null) {
                // do nothing
            } else if (newData.settingsPreview.isEmpty()) {
                newItems.add(Item.EmptyItem)
            } else {
                newData.settingsPreview.mapTo(newItems) {
                    Item.SettingListItem(
                        settingKey = it.key,
                        value = it.value,
                        keyToSettingItems[it.key] ?: listOf(),
                    )
                }
            }

            adapterHelper.setItems(newItems, this)
        }
    }

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
        when (tag) {
            "delete_confirm" -> {
                viewModel.deleteSetting(args.account, dialog.getExtra("setting_key"))
            }
        }
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    }
}
