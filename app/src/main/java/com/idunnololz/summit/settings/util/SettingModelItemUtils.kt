package com.idunnololz.summit.settings.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.ColorSettingItem
import com.idunnololz.summit.settings.DescriptionSettingItem
import com.idunnololz.summit.settings.ImageValueSettingItem
import com.idunnololz.summit.settings.OnOffSettingItem
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.SettingsAdapter
import com.idunnololz.summit.settings.SliderSettingItem
import com.idunnololz.summit.settings.TextValueSettingItem
import com.idunnololz.summit.settings.dialogs.RichTextValueDialogFragment
import com.idunnololz.summit.settings.dialogs.TextValueDialogFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.ext.showAllowingStateLoss

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
