package com.idunnololz.summit.settings.backupAndRestore

import android.content.Context
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.ImportSettingItemBinding
import com.idunnololz.summit.databinding.ImportTableItemBinding
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class SettingDataAdapter(
    private val context: Context,
    private val isImporting: Boolean,
    private val onSettingPreviewClick: (
        settingKey: String,
        settingsDataPreview: SettingsDataPreview,
    ) -> Unit,
    private val onTableClick: (
        tableName: String,
    ) -> Unit,
    private val onDeleteClick: SettingDataAdapter.(
        key: String,
    ) -> Unit,
) : RecyclerView.Adapter<ViewHolder>() {

    private sealed interface Item {
        data class DatabaseTableSummaryItem(
            val tableName: String,
            val rowCount: Int,
            val resolution: ImportTableResolution,
        ) : Item

        /**
         * A single setting within the imported setting data.
         */
        data class ImportSettingItem(
            val settingKey: String,
            val value: Any,
            val isExcluded: Boolean,
        ) : Item

        data object FooterItem : Item
    }

    enum class ImportTableResolution {
        Merge,
        Overwrite,
        Ignore,
    }

    var excludeKeys = mutableSetOf<String>()
    var tableResolutions = mutableMapOf<String, ImportTableResolution>()

    private var settingsDataPreview: SettingsDataPreview? = null

    private val adapterHelper = AdapterHelper<Item>(
        areItemsTheSame = { oldItem, newItem ->
            oldItem::class == newItem::class && when (oldItem) {
                is Item.DatabaseTableSummaryItem ->
                    oldItem.tableName == (newItem as Item.DatabaseTableSummaryItem).tableName
                is Item.ImportSettingItem ->
                    oldItem.settingKey == (newItem as Item.ImportSettingItem).settingKey
                Item.FooterItem -> true
            }
        },
    ).apply {
        addItemType(
            clazz = Item.DatabaseTableSummaryItem::class,
            inflateFn = ImportTableItemBinding::inflate,
        ) { item, b, h ->
            b.icon.setImageResource(R.drawable.outline_backup_table_24)
            b.settingKey.text = item.tableName
            b.settingValue.text = context.getString(
                R.string.row_count_format,
                PrettyPrintUtils.defaultDecimalFormat.format(item.rowCount),
            )

            if (isImporting) {
                b.resolution.visibility = View.VISIBLE
                b.resolution.text = when (item.resolution) {
                    ImportTableResolution.Merge ->
                        context.getString(R.string.import_table_strategy_merge)
                    ImportTableResolution.Overwrite ->
                        context.getString(R.string.import_table_strategy_overwrite)
                    ImportTableResolution.Ignore ->
                        context.getString(R.string.import_table_strategy_ignore)
                }
            } else {
                b.resolution.visibility = View.GONE
            }

            b.resolution.setOnClickListener {
                PopupMenu(context, b.resolution)
                    .apply {
                        menu.add(
                            0,
                            R.id.import_table_strategy_merge,
                            0,
                            R.string.import_table_strategy_merge,
                        )
                        menu.add(
                            0,
                            R.id.import_table_strategy_overwrite,
                            0,
                            R.string.import_table_strategy_overwrite,
                        )
                        menu.add(
                            0,
                            R.id.import_table_strategy_ignore,
                            0,
                            R.string.import_table_strategy_ignore,
                        )

                        setOnMenuItemClickListener {
                            when (it.itemId) {
                                R.id.import_table_strategy_merge -> {
                                    tableResolutions[item.tableName] =
                                        ImportTableResolution.Merge
                                    updateItems()
                                }
                                R.id.import_table_strategy_overwrite -> {
                                    tableResolutions[item.tableName] =
                                        ImportTableResolution.Overwrite
                                    updateItems()
                                }
                                R.id.import_table_strategy_ignore -> {
                                    tableResolutions[item.tableName] =
                                        ImportTableResolution.Ignore
                                    updateItems()
                                }
                            }
                            true
                        }
                    }
                    .show()
            }

            b.root.setOnClickListener {
                onTableClick(item.tableName)
            }
        }
        addItemType(
            clazz = Item.ImportSettingItem::class,
            inflateFn = ImportSettingItemBinding::inflate,
        ) { item, b, h ->
            b.icon.setImageResource(R.drawable.baseline_settings_24)
            b.settingTitle.visibility = View.GONE
            b.settingKey.text = item.settingKey.lowercase()
            b.settingValue.text = item.value.toString()
            b.root.setOnClickListener {
                val settingsDataPreview = settingsDataPreview ?: return@setOnClickListener
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
                    onDeleteClick(item.settingKey)
                    updateItems()
                }
            }
        }
        addItemType(
            Item.FooterItem::class,
            GenericSpaceFooterItemBinding::inflate,
        ) { item, b, h -> }
    }

    init {
        updateItems()
    }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        adapterHelper.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        adapterHelper.onBindViewHolder(holder, position)

    override fun getItemCount(): Int = adapterHelper.itemCount

    private fun updateItems() {
        val newItems = mutableListOf<Item>()
        val newData = settingsDataPreview ?: return

        newData.databaseTablePreview.mapTo(newItems) {
            Item.DatabaseTableSummaryItem(
                tableName = it.value.tableName,
                rowCount = it.value.rowCount,
                resolution = tableResolutions.getOrPut(it.value.tableName) {
                    ImportTableResolution.Merge
                },
            )
        }
        newData.settingsPreview.mapTo(newItems) {
            Item.ImportSettingItem(
                settingKey = it.key,
                value = it.value,
                isExcluded = excludeKeys.contains(it.key),
            )
        }
        newItems.add(Item.FooterItem)

        adapterHelper.setItems(newItems, this)
    }

    fun setData(data: SettingsDataPreview) {
        settingsDataPreview = data

        updateItems()
    }
}
