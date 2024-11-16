package com.idunnololz.summit.settings.backupAndRestore

import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.ImportSettingItemBinding
import com.idunnololz.summit.settings.SettingItemsAdapter.Item
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class SettingDataAdapter(
    private val onSettingPreviewClick: (
        settingKey: String,
        settingsDataPreview: SettingsDataPreview,
    ) -> Unit,
    private val onDeleteClick: SettingDataAdapter.(
        key: String,
    ) -> Unit,
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

        data object FooterItem : Item
    }

    var excludeKeys = mutableSetOf<String>()

    private var settingsDataPreview: SettingsDataPreview? = null

    private val adapterHelper = AdapterHelper<Item>(
        areItemsTheSame = { oldItem, newItem ->
            oldItem::class == newItem::class && when (oldItem) {
                is Item.ImportSettingItem ->
                    oldItem.settingKey == (newItem as Item.ImportSettingItem).settingKey
                Item.FooterItem -> true
            }
        },
    ).apply {
        addItemType(
            clazz = Item.ImportSettingItem::class,
            inflateFn = ImportSettingItemBinding::inflate,
        ) { item, b, h ->
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
