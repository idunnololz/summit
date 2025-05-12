package com.idunnololz.summit.settings

import android.content.Context
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding
import coil3.load
import com.google.android.material.slider.Slider
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.RadioGroupOptionSettingItemBinding
import com.idunnololz.summit.databinding.SettingColorItemBinding
import com.idunnololz.summit.databinding.SettingDividerItemBinding
import com.idunnololz.summit.databinding.SettingImageValueBinding
import com.idunnololz.summit.databinding.SettingItemOnOffBinding
import com.idunnololz.summit.databinding.SettingItemOnOffMasterBinding
import com.idunnololz.summit.databinding.SettingSliderItemBinding
import com.idunnololz.summit.databinding.SettingTextValueBinding
import com.idunnololz.summit.databinding.SubgroupSettingItemBinding
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.settings.RadioGroupSettingItem.RadioGroupOption
import com.idunnololz.summit.settings.dialogs.RichTextValueDialogFragment
import com.idunnololz.summit.settings.dialogs.TextValueDialogFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.SummitActivity
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.showMoreLinkOptions
import io.noties.markwon.Markwon
import java.util.LinkedList
import kotlin.reflect.KClass

sealed interface SettingModelItem {
  val setting: SettingItem

  class RadioGroupItem(
    override val setting: RadioGroupSettingItem,
    val getCurrentValue: () -> Int,
    val onValueChanged: (Int) -> Unit,
  ) : SettingModelItem

  data class SubgroupItem(
    override val setting: com.idunnololz.summit.settings.SubgroupItem,
    val settings: List<SettingModelItem>,
  ) : SettingModelItem {
    constructor(title: String, settings: List<SettingModelItem>) : this(
      SubgroupItem(title, null, listOf(), listOf()),
      settings,
    )
  }

  data class OnOffSwitchItem(
    override val setting: OnOffSettingItem,
    val getCurrentValue: () -> Boolean,
    val onValueChanged: (Boolean) -> Unit,
  ) : SettingModelItem

  data class ColorItem(
    override val setting: ColorSettingItem,
    val defaultValue: Int,
    val getCurrentValue: () -> Int?,
    val onValueChanged: (Int) -> Unit,
  ) : SettingModelItem

  data class CustomItem(
    override val setting: SettingItem,
    val title: String,
    val description: String?,
    val icon: Int,
    val clickable: Boolean,
    val getCurrentValue: () -> String?,
    val onValueChanged: SettingsAdapter.() -> Unit,
  ) : SettingModelItem

  data class CustomViewItem<T, VB : ViewBinding>(
    override val setting: SettingItem,
    val viewBindingClass: KClass<VB>,
    val typeId: Int,
    val createBinding: (LayoutInflater, ViewGroup, Boolean) -> ViewBinding,
    val bindViewHolder: (
      item: SettingsAdapter.Item.CustomViewItem<T>,
      b: VB,
      h: ViewHolder,
    ) -> Unit,
    val payload: T,
  ) : SettingModelItem

  data class ImageValueItem(
    override val setting: ImageValueSettingItem,
    val getCurrentValue: () -> String?,
    val onValueChanged: () -> Unit,
  ) : SettingModelItem

  data class SliderSettingItem(
    override val setting: com.idunnololz.summit.settings.SliderSettingItem,
    val getCurrentValue: () -> Float,
    val onValueChanged: (Float) -> Unit,
  ) : SettingModelItem

  data class DividerItem(
    @IdRes val id: Int,
  ) : SettingModelItem {
    override val setting: SettingItem
      get() = TODO("Not yet implemented")
  }

  data class MasterSwitchItem(
    override val setting: OnOffSettingItem,
    val getCurrentValue: () -> Boolean,
    val onValueChanged: (Boolean) -> Unit,
  ) : SettingModelItem
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
  getCurrentValue: () -> Int?,
  onValueChanged: SettingsAdapter.(RadioGroupSettingItem) -> Unit,
): SettingModelItem.CustomItem {
  return SettingModelItem.CustomItem(
    setting = this,
    title = this.title,
    description = this.description,
    icon = 0,
    clickable = true,
    getCurrentValue = { this.options.firstOrNull { it.id == getCurrentValue() }?.title },
    onValueChanged = { onValueChanged(this@asCustomItem) },
  )
}

fun DescriptionSettingItem.asCustomItem(): SettingModelItem.CustomItem {
  return SettingModelItem.CustomItem(
    setting = this,
    title = this.title,
    description = this.description,
    icon = 0,
    clickable = false,
    getCurrentValue = { null },
    onValueChanged = { },
  )
}

fun BasicSettingItem.asCustomItem(
  onValueChanged: (BasicSettingItem) -> Unit,
): SettingModelItem.CustomItem {
  return SettingModelItem.CustomItem(
    setting = this,
    title = this.title,
    description = this.description,
    icon = this.icon ?: 0,
    clickable = true,
    getCurrentValue = { null },
    onValueChanged = { onValueChanged(this@asCustomItem) },
  )
}

fun BasicSettingItem.asCustomItem(
  drawableRes: Int,
  onValueChanged: (BasicSettingItem) -> Unit,
): SettingModelItem.CustomItem {
  return SettingModelItem.CustomItem(
    setting = this,
    title = this.title,
    description = this.description,
    icon = drawableRes,
    clickable = true,
    getCurrentValue = { null },
    onValueChanged = { onValueChanged(this@asCustomItem) },
  )
}

inline fun <T, reified VB : ViewBinding> BasicSettingItem.asCustomViewSettingsItem(
  typeId: Int,
  payload: T,
  noinline inflateFn: (LayoutInflater, ViewGroup, Boolean) -> VB,
  noinline bindViewHolder: (
    item: SettingsAdapter.Item.CustomViewItem<T>,
    b: VB,
    h: ViewHolder,
  ) -> Unit,
): SettingModelItem.CustomViewItem<T, VB> {
  return SettingModelItem.CustomViewItem(
    setting = this,
    viewBindingClass = VB::class,
    typeId = typeId,
    createBinding = inflateFn,
    bindViewHolder = bindViewHolder,
    payload = payload,
  )
}

fun RadioGroupSettingItem.asSingleChoiceSelectorItem(
  getCurrentValue: () -> Int?,
  onValueChanged: SettingsAdapter.(Int) -> Unit,
): SettingModelItem.CustomItem {
  return asCustomItem(
    getCurrentValue,
    a@{
      val activity = getSummitActivity()
      val curChoice = getCurrentValue()
      val bottomMenu = BottomMenu(activity)
        .apply {
          val idToChoice = mutableMapOf<Int, Int>()
          options.withIndex().forEach { (index, option) ->
            idToChoice[index] = option.id
            addItem(index, option.title)

            if (curChoice == null && option.isDefault) {
              setChecked(index)
            } else if (curChoice == option.id) {
              setChecked(index)
            }
          }

          setTitle(title)

          setOnMenuItemClickListener {
            onValueChanged(requireNotNull(idToChoice[it.id]))
            this@a.onValueChanged()
          }
        }
      activity.showBottomMenu(bottomMenu)
    },
  )
}

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

fun OnOffSettingItem.asOnOffMasterSwitch(
  getCurrentValue: () -> Boolean,
  onValueChanged: (Boolean) -> Unit,
): SettingModelItem {
  return SettingModelItem.MasterSwitchItem(
    this,
    getCurrentValue,
    onValueChanged,
  )
}

fun ColorSettingItem.asColorItem(
  getCurrentValue: () -> Int?,
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
  onValueChanged: SettingsAdapter.() -> Unit,
): SettingModelItem.CustomItem {
  return SettingModelItem.CustomItem(
    setting = this,
    title = title,
    description = description,
    icon = 0,
    clickable = true,
    getCurrentValue = getCurrentValue,
    onValueChanged = onValueChanged,
  )
}

fun TextValueSettingItem.asCustomItemWithTextEditorDialog(
  getCurrentValue: () -> String,
  fragmentManager: FragmentManager,
): SettingModelItem.CustomItem {
  return SettingModelItem.CustomItem(
    setting = this,
    title = title,
    description = description,
    icon = 0,
    clickable = true,
    getCurrentValue = getCurrentValue,
    onValueChanged = {
      val setting = this@asCustomItemWithTextEditorDialog
      if (setting.supportsRichText) {
        RichTextValueDialogFragment.newInstance(
          setting.title,
          setting.id,
          getCurrentValue() as? String,
        ).showAllowingStateLoss(fragmentManager, "asdf")
      } else {
        TextValueDialogFragment.newInstance(
          setting.title,
          setting.id,
          setting.hint,
          getCurrentValue() as? String,
        ).showAllowingStateLoss(fragmentManager, "asdf")
      }
    },
  )
}

fun ImageValueSettingItem.asImageValueItem(
  getCurrentValue: () -> String,
  onValueChanged: () -> Unit,
): SettingModelItem.ImageValueItem {
  return SettingModelItem.ImageValueItem(
    this,
    getCurrentValue,
    onValueChanged,
  )
}

fun SliderSettingItem.asSliderItem(
  getCurrentValue: () -> Float,
  onValueChanged: (Float) -> Unit,
): SettingModelItem.SliderSettingItem {
  return SettingModelItem.SliderSettingItem(
    this,
    getCurrentValue,
    onValueChanged,
  )
}

class SettingsAdapter(
  private val context: Context,
  private val globalStateStorage: GlobalStateStorage,
  val getSummitActivity: () -> SummitActivity,
  val onValueChanged: () -> Unit,
  private val onLinkClick: (url: String, text: String?, linkContext: LinkContext) -> Unit,
) : Adapter<ViewHolder>() {

  sealed interface Item {
    val setting: SettingItem
    val isEnabled: Boolean

    data class RadioGroupOptionItem(
      override val setting: RadioGroupSettingItem,
      override val isEnabled: Boolean,
      val currentValue: Int,
      val option: RadioGroupOption,
    ) : Item

    data class SubtitleItem(
      override val setting: SubgroupItem,
      override val isEnabled: Boolean,
    ) : Item

    data class OnOffSwitchItem(
      override val setting: OnOffSettingItem,
      override val isEnabled: Boolean,
      val currentValue: Boolean,
    ) : Item

    data class MasterSwitchItem(
      override val setting: OnOffSettingItem,
      val currentValue: Boolean,
    ) : Item {
      override val isEnabled: Boolean = true
    }

    data class ColorItem(
      override val setting: ColorSettingItem,
      override val isEnabled: Boolean,
      val currentValue: Int,
      val defaultValue: Int,
    ) : Item

    data class CustomItem(
      override val setting: SettingItem,
      override val isEnabled: Boolean,
      val icon: Int,
      val title: String,
      val description: String?,
      val currentValue: String?,
      val clickable: Boolean,
    ) : Item

    class CustomViewItem<T>(
      override val typeId: Int,
      override val setting: SettingItem,
      override val isEnabled: Boolean,
      val payload: T,
    ) : Item, AdapterHelper.RuntimeItemType

    data class ImageValueItem(
      override val setting: ImageValueSettingItem,
      override val isEnabled: Boolean,
      val value: String?,
    ) : Item

    data class SliderItem(
      override val setting: SliderSettingItem,
      override val isEnabled: Boolean,
      val value: Float,
    ) : Item

    data object FooterItem : Item {
      override val setting: SettingItem
        get() = TODO("Not yet implemented")
      override val isEnabled: Boolean = true
    }

    data class DividerItem(
      @IdRes val id: Int,
    ) : Item {
      override val setting: SettingItem
        get() = TODO("Not yet implemented")
      override val isEnabled: Boolean = true
    }
  }

  private var data: List<SettingModelItem> = listOf()

  private val adapterHelper = AdapterHelper<Item>(
    { old, new ->
      old::class == new::class &&
        when (old) {
          Item.FooterItem -> true
          is Item.DividerItem -> {
            old.id == (new as Item.DividerItem).id
          }
          is Item.SubtitleItem -> {
            old.setting.title == new.setting.title
          }
          else -> {
            old.setting.id == new.setting.id
          }
        }
    },
  ).apply {
    addItemType(
      Item.RadioGroupOptionItem::class,
      RadioGroupOptionSettingItemBinding::inflate,
    ) { item, b, h ->
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

        onValueChanged()
      }
    }
    addItemType(Item.SubtitleItem::class, SubgroupSettingItemBinding::inflate) { item, b, h ->
      b.title.text = item.setting.title
      if (item.setting.description.isNullOrBlank()) {
        b.subtitle.visibility = View.GONE
      } else {
        b.subtitle.visibility = View.VISIBLE
        b.subtitle.text = item.setting.description
      }
    }
    addItemType(Item.OnOffSwitchItem::class, SettingItemOnOffBinding::inflate) { item, b, h ->
      item.setting.bindTo(
        b = b,
        getCurrentValue = { item.currentValue },
        onValueChanged = {
          findSettingModel<SettingModelItem.OnOffSwitchItem>(item.setting.id)
            ?.onValueChanged
            ?.invoke(it)
          onValueChanged()
        },
      )

      b.desc.movementMethod = CustomLinkMovementMethod().apply {
        onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
          override fun onClick(
            textView: TextView,
            url: String,
            text: String,
            rect: RectF,
          ): Boolean {
            onLinkClick(url, text, LinkContext.Text)
            return true
          }
        }
        onLinkLongClickListener = DefaultLinkLongClickListener(context) { url, text ->
          getSummitActivity().showMoreLinkOptions(url, text)
        }
      }
    }
    addItemType(
      Item.MasterSwitchItem::class,
      SettingItemOnOffMasterBinding::inflate,
    ) { item, b, h ->
      val setting = item.setting
      b.title.text = setting.title
      if (setting.description != null) {
        b.desc.visibility = View.VISIBLE

        Markwon.create(b.root.context).setMarkdown(b.desc, setting.description)
      } else {
        b.desc.visibility = View.GONE
      }

      // Unbind previous binding
      b.switchView.setOnCheckedChangeListener(null)
      b.switchView.isChecked = item.currentValue
      b.switchView.jumpDrawablesToCurrentState()
      b.switchView.setOnCheckedChangeListener { compoundButton, newValue ->
        findSettingModel<SettingModelItem.MasterSwitchItem>(item.setting.id)
          ?.onValueChanged
          ?.invoke(newValue)

        onValueChanged()
      }
      b.card.setOnClickListener {
        b.switchView.performClick()
      }

      // Prevent auto state restoration since multiple checkboxes can have the same id
      b.switchView.isSaveEnabled = false
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

          onValueChanged()
        },
        defaultValue = { item.defaultValue },
      )
    }
    addItemType(Item.ImageValueItem::class, SettingImageValueBinding::inflate) { item, b, h ->
      val settingItem = item.setting

      b.title.text = settingItem.title
      if (settingItem.description != null) {
        b.desc.visibility = View.VISIBLE
        b.desc.text = settingItem.description
      } else {
        b.desc.visibility = View.GONE
      }

      b.imageView.load(item.value)

      b.root.tag = settingItem
      b.root.setOnClickListener {
        findSettingModel<SettingModelItem.ImageValueItem>(item.setting.id)
          ?.onValueChanged
          ?.invoke()

        onValueChanged()
      }
    }
    addItemType(Item.CustomItem::class, SettingTextValueBinding::inflate) { item, b, h ->
      if (item.icon == 0) {
        b.icon.visibility = View.GONE
      } else {
        b.icon.visibility = View.VISIBLE
        b.icon.setImageResource(item.icon)
      }

      if (item.title.isBlank()) {
        b.title.visibility = View.GONE
      } else {
        b.title.visibility = View.VISIBLE
        b.title.text = item.title
      }

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

      if (item.setting.isEnabled) {
        b.title.isEnabled = true
        b.desc.isEnabled = true
        b.value.isEnabled = true
        b.root.isEnabled = true
        if (item.clickable) {
          b.root.setOnClickListener {
            findSettingModel<SettingModelItem.CustomItem>(item.setting.id)
              ?.onValueChanged
              ?.invoke(this@SettingsAdapter)

            onValueChanged()
          }
        } else {
          b.root.isClickable = false
          b.root.setOnClickListener(null)
        }
      } else {
        b.title.isEnabled = false
        b.desc.isEnabled = false
        b.value.isEnabled = false
        b.root.isEnabled = false
      }
    }
    addItemType(Item.SliderItem::class, SettingSliderItemBinding::inflate) { item, b, h ->
      val setting = item.setting
      b.title.text = setting.title
      b.slider.valueFrom = setting.minValue
      b.slider.valueTo = setting.maxValue

      val stepSize = setting.stepSize
      if (stepSize != null) {
        b.slider.stepSize = stepSize
        b.slider.value =
          ((item.value / stepSize).toInt() * b.slider.stepSize)
            .coerceIn(setting.minValue, setting.maxValue)
      } else {
        b.slider.value = item.value.coerceIn(setting.minValue, setting.maxValue)
      }

      b.slider.addOnSliderTouchListener(
        object : Slider.OnSliderTouchListener {
          override fun onStartTrackingTouch(slider: Slider) {}

          override fun onStopTrackingTouch(slider: Slider) {
            findSettingModel<SettingModelItem.SliderSettingItem>(item.setting.id)
              ?.onValueChanged
              ?.invoke(slider.value)

            onValueChanged()
          }
        },
      )

      // Prevent auto state restoration since multiple checkboxes can have the same id
      b.slider.isSaveEnabled = false
    }

    addItemType(Item.FooterItem::class, GenericSpaceFooterItemBinding::inflate) { item, b, h -> }
    addItemType(Item.DividerItem::class, SettingDividerItemBinding::inflate) { item, b, h -> }
  }

  private inline fun <reified T : SettingModelItem> findSettingModel(id: Int): T? {
    val data = LinkedList(data)
    while (data.isNotEmpty()) {
      val d = data.removeFirst()
      if (d is SettingModelItem.SubgroupItem) {
        data.addAll(d.settings)
      }
      if (d is SettingModelItem.DividerItem) {
        // dividers are not settings!
      } else {
        if (d.setting.id == id) {
          return d as? T
        }
      }
    }
    return null
  }

  override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    adapterHelper.onCreateViewHolder(parent, viewType)

  override fun getItemCount(): Int = adapterHelper.itemCount

  override fun onBindViewHolder(holder: ViewHolder, position: Int) =
    adapterHelper.onBindViewHolder(holder, position)

  fun setData(data: List<SettingModelItem>) {
    this.data = data

    refreshItems()
  }

  private fun refreshItems() {
    val data = data
    val newItems = mutableListOf<Item>()

    val masterSwitch = data.firstOrNull { it is SettingModelItem.MasterSwitchItem }
    val masterSwitchEnabled = masterSwitch?.setting?.isEnabled ?: true

    fun processItem(item: SettingModelItem) {
      when (item) {
        is SettingModelItem.RadioGroupItem -> {
          item.setting.options.forEach {
            newItems += Item.RadioGroupOptionItem(
              setting = item.setting,
              isEnabled = item.setting.isEnabled && masterSwitchEnabled,
              currentValue = item.getCurrentValue(),
              option = it,
            )
          }
        }
        is SettingModelItem.SubgroupItem -> {
          newItems += Item.SubtitleItem(
            setting = item.setting,
            isEnabled = item.setting.isEnabled && masterSwitchEnabled,
          )
          item.settings.forEach {
            processItem(it)
          }
        }
        is SettingModelItem.OnOffSwitchItem -> {
          newItems += Item.OnOffSwitchItem(
            setting = item.setting,
            isEnabled = item.setting.isEnabled && masterSwitchEnabled,
            currentValue = item.getCurrentValue(),
          )
        }
        is SettingModelItem.ColorItem -> {
          newItems += Item.ColorItem(
            setting = item.setting,
            isEnabled = item.setting.isEnabled && masterSwitchEnabled,
            currentValue = item.getCurrentValue() ?: item.defaultValue,
            defaultValue = item.defaultValue,
          )
        }
        is SettingModelItem.CustomItem -> {
          newItems += Item.CustomItem(
            setting = item.setting,
            isEnabled = item.setting.isEnabled && masterSwitchEnabled,
            icon = item.icon,
            title = item.title,
            description = item.description,
            currentValue = item.getCurrentValue(),
            clickable = item.clickable,
          )
        }
        is SettingModelItem.CustomViewItem<*, *> -> {
          val castedItem = item as SettingModelItem.CustomViewItem<Any, ViewBinding>
          adapterHelper.addItemTypeInternal(
            clazz = Item.CustomViewItem::class,
            viewBindingClass = item.viewBindingClass,
            inflateFn = item.createBinding,
            bindViewHolder = { i: Item.CustomViewItem<Any>, b: Any, h: ViewHolder ->
              castedItem.bindViewHolder(i, b as ViewBinding, h)
            },
            id = item.typeId,
            onViewCreated = {},
          )

          newItems += Item.CustomViewItem(
            setting = item.setting,
            isEnabled = item.setting.isEnabled && masterSwitchEnabled,
            typeId = item.typeId,
            payload = item.payload,
          )
        }

        is SettingModelItem.ImageValueItem -> {
          newItems += Item.ImageValueItem(
            setting = item.setting,
            isEnabled = item.setting.isEnabled && masterSwitchEnabled,
            value = item.getCurrentValue(),
          )
        }
        is SettingModelItem.SliderSettingItem -> {
          newItems += Item.SliderItem(
            setting = item.setting,
            isEnabled = item.setting.isEnabled && masterSwitchEnabled,
            value = item.getCurrentValue(),
          )
        }
        is SettingModelItem.DividerItem -> {
          newItems += Item.DividerItem(item.id)
        }
        is SettingModelItem.MasterSwitchItem -> {
          newItems += Item.MasterSwitchItem(item.setting, item.getCurrentValue())
        }
      }
    }

    data.forEach { item ->
      processItem(item)
    }

    newItems += Item.FooterItem

    adapterHelper.setItems(newItems, this)
  }
}
