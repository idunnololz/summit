package com.idunnololz.summit.settings

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.databinding.RadioGroupOptionSettingItemBinding
import com.idunnololz.summit.databinding.SettingColorItemBinding
import com.idunnololz.summit.databinding.SettingItemOnOffBinding
import com.idunnololz.summit.databinding.SettingTextValueBinding
import com.idunnololz.summit.databinding.SubgroupSettingItemBinding
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.settings.RadioGroupSettingItem.RadioGroupOption
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.SummitActivity
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import java.util.LinkedList


sealed interface SettingModelItem {
  val setting: SettingItem

  class RadioGroupItem(
    override val setting: RadioGroupSettingItem,
    val getCurrentValue: () -> Int,
    val onValueChanged: (Int) -> Unit,
  ): SettingModelItem

  data class SubgroupItem(
    override val setting: com.idunnololz.summit.settings.SubgroupItem,
    val settings: List<SettingModelItem>,
  ) : SettingModelItem {
    constructor(title: String, settings: List<SettingModelItem>): this(
      SubgroupItem(title, listOf(), listOf()),
      settings
    )
  }

  data class OnOffSwitchItem(
    override val setting: OnOffSettingItem,
    val getCurrentValue: () -> Boolean,
    val onValueChanged: (Boolean) -> Unit,
  ): SettingModelItem

  data class ColorItem(
    override val setting: ColorSettingItem,
    val defaultValue: Int,
    val getCurrentValue: () -> Int,
    val onValueChanged: (Int) -> Unit,
  ): SettingModelItem

  data class CustomItem(
    override val setting: SettingItem,
    val title: String,
    val description: String?,
    val icon: Int,
    val getCurrentValue: () -> String?,
    val onValueChanged: () -> Unit,
  ): SettingModelItem
}

fun RadioGroupSettingItem.asRadioGroup(
  getCurrentValue: () -> Int,
  onValueChanged: (Int) -> Unit,
): SettingModelItem.RadioGroupItem {
  return SettingModelItem.RadioGroupItem(
    this,
    getCurrentValue,
    onValueChanged,
  )
}

fun RadioGroupSettingItem.asCustomItem(
  getCurrentValue: () -> Int,
  onValueChanged: (RadioGroupSettingItem) -> Unit,
): SettingModelItem.CustomItem {
  return SettingModelItem.CustomItem(
    this,
    this.title,
    this.description,
    0,
    { this.options.firstOrNull { it.id == getCurrentValue() }?.title },
    { onValueChanged(this) },
  )
}

fun BasicSettingItem.asCustomItem(
  onValueChanged: (BasicSettingItem) -> Unit,
): SettingModelItem.CustomItem {
  return SettingModelItem.CustomItem(
    this,
    this.title,
    this.description,
    0,
    { null },
    { onValueChanged(this) },
  )
}

fun BasicSettingItem.asCustomItem(
  drawableRes: Int,
  onValueChanged: (BasicSettingItem) -> Unit,
): SettingModelItem.CustomItem {
  return SettingModelItem.CustomItem(
    this,
    this.title,
    this.description,
    drawableRes,
    { null },
    { onValueChanged(this) },
  )
}

fun RadioGroupSettingItem.asSingleChoiceSelectorItem(
  activity: SummitActivity,
  getCurrentValue: () -> Int,
  onValueChanged: (Int) -> Unit,
): SettingModelItem.CustomItem =
  asCustomItem(
    getCurrentValue,
    {
      val curChoice = getCurrentValue()
      val bottomMenu = BottomMenu(activity)
        .apply {
          val idToChoice = mutableMapOf<Int, Int>()
          options.withIndex().forEach { (index, option) ->
            idToChoice[index] = option.id
            addItem(index, option.title)

            if (curChoice == option.id) {
              setChecked(index)
            }
          }

          setTitle(title)

          setOnMenuItemClickListener {
            onValueChanged(requireNotNull(idToChoice[it.id]))
          }
        }
      activity.showBottomMenu(bottomMenu)
    }
  )

fun OnOffSettingItem.asOnOffSwitch(
  getCurrentValue: () -> Boolean,
  onValueChanged: (Boolean) -> Unit,
): SettingModelItem {
  return SettingModelItem.OnOffSwitchItem(
    this,
    getCurrentValue,
    onValueChanged,
  )
}

fun ColorSettingItem.asColorItem(
  getCurrentValue: () -> Int,
  onValueChanged: (Int) -> Unit,
  defaultColor: () -> Int,
): SettingModelItem {
  return SettingModelItem.ColorItem(
    this,
    defaultColor(),
    getCurrentValue,
    onValueChanged,
  )
}

fun TextValueSettingItem.asCustomItem(
  getCurrentValue: () -> String,
  onValueChanged: () -> Unit,
): SettingModelItem.CustomItem {
  return SettingModelItem.CustomItem(
    this,
    title,
    description,
    0,
    getCurrentValue,
    onValueChanged,
  )
}

class SettingsAdapter(
  private val globalStateStorage: GlobalStateStorage,
) : Adapter<ViewHolder>() {

  sealed interface Item {
    val setting: SettingItem

    data class RadioGroupOptionItem(
      override val setting: RadioGroupSettingItem,
      val currentValue: Int,
      val option: RadioGroupOption,
    ): Item

    data class SubtitleItem(
      override val setting: SubgroupItem,
    ): Item

    data class OnOffSwitchItem(
      override val setting: OnOffSettingItem,
      val currentValue: Boolean,
    ): Item

    data class ColorItem(
      override val setting: ColorSettingItem,
      val currentValue: Int,
      val defaultValue: Int,
    ): Item

    data class CustomItem(
      override val setting: SettingItem,
      val icon: Int,
      val title: String,
      val description: String?,
      val currentValue: String?,
    ): Item
  }

  private var data: List<SettingModelItem> = listOf()

  private val adapterHelper = AdapterHelper<Item>(
    { old, new ->
      old::class == new::class && old.setting.id == new.setting.id
    }
  ).apply {
    addItemType(Item.RadioGroupOptionItem::class, RadioGroupOptionSettingItemBinding::inflate) { item, b, h ->
      val setting = item.setting
      val option = item.option

      b.title.text = option.title

      b.radioButton.isChecked = option.id == item.currentValue

      if (option.icon != null) {
        b.title.setCompoundDrawablesRelativeWithIntrinsicBounds(option.icon, 0, 0, 0)
      } else {
        b.title.setCompoundDrawables(null, null, null, null)
      }

      if (option.description != null) {
        b.desc.visibility = View.VISIBLE
        b.desc.text = option.description
      } else {
        b.desc.visibility = View.GONE
      }

      b.radioButton.setOnCheckedChangeListener { _, value ->
        findSettingModel<SettingModelItem.RadioGroupItem>(setting.id)
          ?.onValueChanged
          ?.invoke(option.id)

        refreshItems()
      }
    }
    addItemType(Item.SubtitleItem::class, SubgroupSettingItemBinding::inflate) { item, b, h ->
      b.title.text = item.setting.title
    }
    addItemType(Item.OnOffSwitchItem::class, SettingItemOnOffBinding::inflate) { item, b, h ->
      item.setting.bindTo(
        b = b,
        getCurrentValue = { item.currentValue },
        onValueChanged = {
          findSettingModel<SettingModelItem.OnOffSwitchItem>(item.setting.id)
            ?.onValueChanged
            ?.invoke(it)

          refreshItems()
        }
      )
    }
    addItemType(Item.ColorItem::class, SettingColorItemBinding::inflate) { item, b, h ->
      item.setting.bindTo(
        b = b,
        globalStateStorage = globalStateStorage,
        getCurrentValue = { item.currentValue },
        onValueChanged = {
          findSettingModel<SettingModelItem.ColorItem>(item.setting.id)
            ?.onValueChanged
            ?.invoke(it)
          refreshItems()
        },
        defaultValue = { item.defaultValue },
      )
    }
    addItemType(Item.CustomItem::class, SettingTextValueBinding::inflate) { item, b, h ->
      if (item.icon == 0) {
        b.icon.visibility = View.GONE
      } else {
        b.icon.visibility = View.VISIBLE
        b.icon.setImageResource(item.icon)
      }

      b.title.text = item.title

      if (item.description.isNullOrBlank()) {
        b.desc.visibility = View.GONE
      } else {
        b.desc.text = item.description
        b.desc.visibility = View.VISIBLE
      }

      if (item.currentValue.isNullOrBlank()) {
        b.value.visibility = View.GONE
      } else {
        b.value.text = item.currentValue
        b.value.visibility = View.VISIBLE
      }

      b.root.tag = this
      b.root.setOnClickListener {
        findSettingModel<SettingModelItem.CustomItem>(item.setting.id)
          ?.onValueChanged
          ?.invoke()
        refreshItems()
      }
    }
  }

  private inline fun <reified T : SettingModelItem> findSettingModel(id: Int): T? {
    val data = LinkedList(data)
    while(data.isNotEmpty()) {
      val d = data.removeFirst()
      if (d is SettingModelItem.SubgroupItem) {
        data.addAll(d.settings)
      }
      if (d.setting.id == id) {
        return d as? T
      }
    }
    return null
  }

  override fun getItemViewType(position: Int): Int =
    adapterHelper.getItemViewType(position)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    adapterHelper.onCreateViewHolder(parent, viewType)

  override fun getItemCount(): Int =
    adapterHelper.itemCount

  override fun onBindViewHolder(holder: ViewHolder, position: Int) =
    adapterHelper.onBindViewHolder(holder, position)

  fun setData(data: List<SettingModelItem>) {
    this.data = data

    refreshItems()
  }

  private fun refreshItems() {
    val data = data
    val newItems = mutableListOf<Item>()

    fun processItem(item: SettingModelItem) {
      when (item) {
        is SettingModelItem.RadioGroupItem -> {
          item.setting.options.forEach {
            newItems += Item.RadioGroupOptionItem(item.setting, item.getCurrentValue(), it)
          }
        }
        is SettingModelItem.SubgroupItem -> {
          newItems += Item.SubtitleItem(
            item.setting
          )
          item.settings.forEach {
            processItem(it)
          }
        }
        is SettingModelItem.OnOffSwitchItem -> {
          newItems += Item.OnOffSwitchItem(item.setting, item.getCurrentValue())
        }
        is SettingModelItem.ColorItem -> {
          newItems += Item.ColorItem(item.setting, item.getCurrentValue(), item.defaultValue)
        }
        is SettingModelItem.CustomItem -> {
          newItems += Item.CustomItem(
            setting = item.setting,
            icon = item.icon,
            title = item.title,
            description = item.description,
            currentValue = item.getCurrentValue()
          )
        }
      }
    }

    data.forEach { item ->
      processItem(item)
    }

    adapterHelper.setItems(newItems, this)
  }
}