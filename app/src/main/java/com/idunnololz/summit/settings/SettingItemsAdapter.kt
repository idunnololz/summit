package com.idunnololz.summit.settings

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.text.buildSpannedString
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.BasicSettingItemBinding
import com.idunnololz.summit.databinding.SettingDescriptionItemBinding
import com.idunnololz.summit.databinding.SettingImageValueBinding
import com.idunnololz.summit.databinding.SettingOnOffBinding
import com.idunnololz.summit.databinding.SettingTextValueBinding
import com.idunnololz.summit.databinding.SliderSettingItemBinding
import com.idunnololz.summit.databinding.SubgroupSettingItemBinding
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.RichTextValueDialogFragment
import com.idunnololz.summit.settings.dialogs.TextValueDialogFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class SettingItemsAdapter(
    private val context: Context,
    private val onSettingClick: (SettingItem) -> Boolean,
    private val fragmentManager: FragmentManager,
    private val onImagePickerClick: ((ImageValueSettingItem) -> Unit)? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed interface Item {
        val settingItem: SettingItem

        data class BasicItem(
            override val settingItem: BasicSettingItem,
        ) : Item

        data class TextValueItem(
            override val settingItem: TextValueSettingItem,
            val value: String?,
        ) : Item

        data class TitleItem(
            override val settingItem: SubgroupItem,
            val hasTopMargin: Boolean,
        ) : Item

        data class DescriptionItem(
            override val settingItem: DescriptionSettingItem,
        ) : Item

        data class RadioGroupItem(
            override val settingItem: RadioGroupSettingItem,
            @IdRes val value: Int?,
        ) : Item

        data class OnOffItem(
            override val settingItem: OnOffSettingItem,
            val value: Boolean?,
        ) : Item

        data class ImageValueItem(
            override val settingItem: ImageValueSettingItem,
            val value: String?,
        ) : Item

        data class SliderItem(
            override val settingItem: SliderSettingItem,
            val value: Float?,
        ) : Item

        data class ColorItem(
            override val settingItem: ColorSettingItem,
            @IdRes val value: Int?,
        ) : Item
    }

    var data: List<SettingItem> = listOf()
        private set

    var defaultSettingValues: Map<Int, Any?> = mapOf()
        set(value) {
            field = value

            refreshItems()
        }

    var firstTitleHasTopMargin: Boolean = true

    private val _updatedSettingValues = mutableMapOf<Int, Any?>()
    val updatedSettingValues: Map<Int, Any?>
        get() = _updatedSettingValues

    var settingsChanged: (() -> Unit)? = null

    private val onSettingClickListener = View.OnClickListener {
        val settingItem = it.tag as SettingItem
        val handled = onSettingClick(settingItem)
        val value = getCurrentValue(settingItem.id)

        if (!handled) {
            when (settingItem) {
                is BasicSettingItem -> {}
                is OnOffSettingItem -> {
                    updateSettingValue(settingItem.id, (it as MaterialSwitch).isChecked)
                }
                is RadioGroupSettingItem -> {
                    MultipleChoiceDialogFragment.newInstance(settingItem, null)
                        .showAllowingStateLoss(fragmentManager, "aaaaaaa")
                }
                is SliderSettingItem -> {
                    updateSettingValue(settingItem.id, (it as Slider).value)
                }
                is SubgroupItem -> TODO()
                is TextOnlySettingItem -> TODO()
                is TextValueSettingItem -> {
                    if (settingItem.supportsRichText) {
                        RichTextValueDialogFragment.newInstance(
                            settingItem.title,
                            settingItem.id,
                            value as? String,
                        ).showAllowingStateLoss(fragmentManager, "asdf")
                    } else {
                        TextValueDialogFragment.newInstance(
                            settingItem.title,
                            settingItem.id,
                            settingItem.hint,
                            value as? String,
                        ).showAllowingStateLoss(fragmentManager, "asdf")
                    }
                }
                is ImageValueSettingItem -> {
                    onImagePickerClick?.invoke(settingItem)
                }
                is ColorSettingItem -> {
                }

                is DescriptionSettingItem -> {}
            }
        } else {
        }
    }

    private val adapterHelper = AdapterHelper<Item>(
        areItemsTheSame = { old, new ->
            old.settingItem.id == new.settingItem.id
        },
    ).apply {
        addItemType(Item.BasicItem::class, BasicSettingItemBinding::inflate) { item, b, h ->
            val settingItem = item.settingItem

            b.root.tag = settingItem
            settingItem.bindTo(b) {
                onSettingClickListener.onClick(b.root)
            }
        }
        addItemType(Item.TitleItem::class, SubgroupSettingItemBinding::inflate) { item, b, h ->
            val settingItem = item.settingItem

            b.title.text = settingItem.title

            if (item.hasTopMargin) {
                b.title.updatePadding(top = context.getDimen(R.dimen.padding_three))
            } else {
                b.title.updatePadding(top = context.getDimen(R.dimen.padding))
            }
        }
        addItemType(Item.TextValueItem::class, SettingTextValueBinding::inflate) { item, b, h ->
            val settingItem = item.settingItem

            b.title.text = settingItem.title
            b.value.text = item.value
                ?: buildSpannedString {
                    append(context.getString(R.string.empty))

                    setSpan(
                        StyleSpan(Typeface.ITALIC),
                        0,
                        length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }

            if (settingItem.isEnabled) {
                b.title.alpha = 1f
                b.value.alpha = 1f
            } else {
                b.title.alpha = 0.66f
                b.value.alpha = 0.66f
            }

            if (settingItem.description != null) {
                b.desc.visibility = View.VISIBLE
                b.desc.text = settingItem.description
            } else {
                b.desc.visibility = View.GONE
            }

            b.root.isEnabled = settingItem.isEnabled
            b.root.tag = settingItem
            b.root.setOnClickListener(onSettingClickListener)
        }
        addItemType(Item.RadioGroupItem::class, SettingTextValueBinding::inflate) { item, b, _ ->
            val settingItem = item.settingItem

            b.title.text = settingItem.title
            b.value.text = settingItem.options.firstOrNull { it.id == item.value }?.title

            if (settingItem.description != null) {
                b.desc.visibility = View.VISIBLE
                b.desc.text = settingItem.description
            } else {
                b.desc.visibility = View.GONE
            }

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
        addItemType(Item.ImageValueItem::class, SettingImageValueBinding::inflate) { item, b, h ->
            val settingItem = item.settingItem

            b.title.text = settingItem.title
            if (settingItem.description != null) {
                b.desc.visibility = View.VISIBLE
                b.desc.text = settingItem.description
            } else {
                b.desc.visibility = View.GONE
            }

            b.imageView.load(item.value)

            b.root.tag = settingItem
            b.root.setOnClickListener(onSettingClickListener)
        }
        addItemType(Item.SliderItem::class, SliderSettingItemBinding::inflate) { item, b, h ->
            val settingItem = item.settingItem

            b.root.tag = settingItem
            settingItem.bindTo(
                b,
                { item.value ?: settingItem.minValue },
                { onSettingClickListener },
            )
        }
        addItemType(
            clazz = Item.DescriptionItem::class,
            inflateFn = SettingDescriptionItemBinding::inflate,
        ) { item, b, h ->
            val settingItem = item.settingItem

            if (settingItem.title.isBlank()) {
                b.title.visibility = View.GONE
            } else {
                b.title.visibility = View.VISIBLE
                b.title.text = settingItem.title
            }
            if (settingItem.description.isNullOrBlank()) {
                b.desc.visibility = View.GONE
            } else {
                b.desc.visibility = View.VISIBLE
                b.desc.text = settingItem.description
            }
        }
    }

    init {
        refreshItems()
    }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        adapterHelper.onBindViewHolder(holder, position)

    private fun refreshItems(onItemsUpdated: () -> Unit = {}) {
        val newItems = mutableListOf<Item>()

        data.forEach {
            addRecursive(it, newItems)
        }

        adapterHelper.setItems(newItems, this, onItemsUpdated)
    }

    private fun getCurrentValue(key: Int): Any? = if (_updatedSettingValues.contains(key)) {
        _updatedSettingValues[key]
    } else if (defaultSettingValues.contains(key)) {
        defaultSettingValues[key]
    } else {
        null
    }

    private fun addRecursive(settingItem: SettingItem, out: MutableList<Item>) {
        when (settingItem) {
            is SubgroupItem -> {
                out.add(
                    Item.TitleItem(
                        settingItem,
                        firstTitleHasTopMargin || out.isNotEmpty(),
                    ),
                )
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
            is SliderSettingItem -> {
                val value = getCurrentValue(settingItem.id)

                out.add(Item.SliderItem(settingItem, value as Float?))
            }
            is TextOnlySettingItem -> TODO()
            is TextValueSettingItem -> {
                val value = getCurrentValue(settingItem.id)

                out.add(Item.TextValueItem(settingItem, value as String?))
            }
            is ImageValueSettingItem -> {
                val value = getCurrentValue(settingItem.id)

                out.add(Item.ImageValueItem(settingItem, value as String?))
            }
            is ColorSettingItem -> {
                val value = getCurrentValue(settingItem.id)

                out.add(Item.ColorItem(settingItem, value as Int?))
            }

            is DescriptionSettingItem -> {
                out.add(Item.DescriptionItem(settingItem))
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

    fun setData(data: List<SettingItem>, onItemsUpdated: () -> Unit = {}) {
        this.data = data

        refreshItems {
            onItemsUpdated()
        }
    }
}
