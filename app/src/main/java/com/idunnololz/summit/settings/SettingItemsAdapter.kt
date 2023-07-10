package com.idunnololz.summit.settings

import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.idunnololz.summit.databinding.BasicSettingItemBinding
import com.idunnololz.summit.databinding.SettingOnOffBinding
import com.idunnololz.summit.databinding.SettingTextValueBinding
import com.idunnololz.summit.databinding.SubgroupSettingItemBinding
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.TextValueDialogFragment
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class SettingItemsAdapter(
    private val onSettingClick: (Int) -> Boolean,
    private val fragmentManager: FragmentManager,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed interface Item {
        val settingItem: SettingItem

        data class BasicItem(
            override val settingItem: BasicSettingItem
        ): Item

        data class TextValueItem(
            override val settingItem: TextValueSettingItem,
            val value: String?,
        ): Item

        data class TitleItem(
            override val settingItem: SubgroupItem
        ): Item

        data class RadioGroupItem(
            override val settingItem: RadioGroupSettingItem,
            @IdRes val value: Int?,
        ): Item

        data class OnOffItem(
            override val settingItem: OnOffSettingItem,
            val value: Boolean?,
        ): Item
    }

    var data: List<SettingItem> = listOf()
        set(value) {
            field = value

            refreshItems()
        }

    var defaultSettingValues: Map<Int, Any?> = mapOf()
        set(value) {
            field = value

            refreshItems()
        }
    private val _updatedSettingValues = mutableMapOf<Int, Any?>()
    val updatedSettingValues: Map<Int, Any?>
        get() = _updatedSettingValues

    var settingsChanged: (() -> Unit)? = null

    private val onSettingClickListener = View.OnClickListener {
        val settingItem = it.tag as SettingItem
        val handled = onSettingClick(settingItem.id)
        val value = getCurrentValue(settingItem.id)

        if (!handled) {
            when (settingItem) {
                is BasicSettingItem -> TODO()
                is OnOffSettingItem -> {
                    updateSettingValue(settingItem.id, (it as MaterialSwitch).isChecked)
                }
                is RadioGroupSettingItem -> {
                    MultipleChoiceDialogFragment.newInstance(settingItem)
                        .showAllowingStateLoss(fragmentManager, "aaaaaaa")
                }
                is SliderSettingItem -> TODO()
                is SubgroupItem -> TODO()
                is TextOnlySettingItem -> TODO()
                is TextValueSettingItem -> {
                    TextValueDialogFragment.newInstance(
                        settingItem.title,
                        settingItem.id,
                        value as? String,
                    ).showAllowingStateLoss(fragmentManager, "asdf")
                }
            }
        }
    }

    private val adapterHelper = AdapterHelper<Item>(
        areItemsTheSame = { old, new ->
            old.settingItem.id == new.settingItem.id
        }
    ).apply {
        addItemType(Item.BasicItem::class, BasicSettingItemBinding::inflate) { item, b, h ->
            val settingItem = item.settingItem

            b.icon.setImageResource(settingItem.icon)
            b.title.text = settingItem.title
            b.desc.text = settingItem.description

            b.root.tag = settingItem
            b.root.setOnClickListener(onSettingClickListener)
        }
        addItemType(Item.TitleItem::class, SubgroupSettingItemBinding::inflate) { item, b, h ->
            val settingItem = item.settingItem

            b.title.text = settingItem.title
        }
        addItemType(Item.TextValueItem::class, SettingTextValueBinding::inflate) { item, b, h ->
            val settingItem = item.settingItem

            b.title.text = settingItem.title
            b.value.text = item.value

            if (settingItem.isEnabled) {
                b.title.alpha = 1f
                b.value.alpha = 1f
            } else {
                b.title.alpha = 0.66f
                b.value.alpha = 0.66f
            }

            b.root.isEnabled = settingItem.isEnabled
            b.root.tag = settingItem
            b.root.setOnClickListener(onSettingClickListener)
        }
        addItemType(Item.RadioGroupItem::class, SettingTextValueBinding::inflate) { item, b, h ->
            val settingItem = item.settingItem

            b.title.text = settingItem.title
            b.value.text = settingItem.options.firstOrNull { it.id == item.value }?.title

            b.root.tag = settingItem
            b.root.setOnClickListener(onSettingClickListener)
        }
        addItemType(Item.OnOffItem::class, SettingOnOffBinding::inflate) { item, b, h ->
            val settingItem = item.settingItem

            b.title.text = settingItem.title
            if (settingItem.description != null) {
                b.desc.visibility = View.VISIBLE
                b.desc.text = settingItem.description
            } else {
                b.desc.visibility = View.GONE
            }
            b.switchView.isChecked = item.value ?: false

            b.switchView.tag = settingItem
            b.switchView.setOnClickListener(onSettingClickListener)
        }
    }

    init {
        refreshItems()
    }

    override fun getItemViewType(position: Int): Int =
        adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int =
        adapterHelper.itemCount

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        adapterHelper.onBindViewHolder(holder, position)

    private fun refreshItems() {
        val newItems = mutableListOf<Item>()

        data.forEach {
            addRecursive(it, newItems)
        }

        adapterHelper.setItems(newItems, this)
    }

    private fun getCurrentValue(key: Int): Any? =
        if (_updatedSettingValues.contains(key)) {
            _updatedSettingValues[key]
        } else if (defaultSettingValues.contains(key)) {
            defaultSettingValues[key]
        } else {
            null
        }

    private fun addRecursive(settingItem: SettingItem, out: MutableList<Item>) {
        when (settingItem) {
            is SubgroupItem -> {
                out.add(Item.TitleItem(settingItem))
                settingItem.settings.forEach {
                    addRecursive(it, out)
                }
            }
            is BasicSettingItem -> {
                out.add(Item.BasicItem(settingItem))
            }
            is OnOffSettingItem -> {
                out.add(Item.OnOffItem(settingItem, getCurrentValue(settingItem.id) as Boolean?))
            }
            is RadioGroupSettingItem -> {
                val value = getCurrentValue(settingItem.id)

                out.add(Item.RadioGroupItem(settingItem, value as Int?))
            }
            is SliderSettingItem -> TODO()
            is TextOnlySettingItem -> TODO()
            is TextValueSettingItem -> {
                val value = getCurrentValue(settingItem.id)

                out.add(Item.TextValueItem(settingItem, value as String?))
            }
        }
    }

    fun updateSettingValue(key: Int, value: Any?) {
        val defaultValue = defaultSettingValues[key]
        if (defaultValue == value) {
            _updatedSettingValues.remove(key)
            refreshItems()
            return
        }

        _updatedSettingValues[key] = value
        refreshItems()
        settingsChanged?.invoke()
    }

    fun changesCommitted() {
        val newDefaults = defaultSettingValues.toMutableMap()
        for ((k, v) in _updatedSettingValues) {
            newDefaults[k] = v
        }
        _updatedSettingValues.clear()
        defaultSettingValues = newDefaults
        settingsChanged?.invoke()
    }
}