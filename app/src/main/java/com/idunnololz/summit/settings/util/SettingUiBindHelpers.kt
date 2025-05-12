package com.idunnololz.summit.settings.util

import android.view.View
import com.google.android.material.slider.Slider
import com.idunnololz.summit.databinding.BasicSettingItemBinding
import com.idunnololz.summit.databinding.RadioGroupOptionSettingItemBinding
import com.idunnololz.summit.databinding.RadioGroupTitleSettingItemBinding
import com.idunnololz.summit.databinding.SettingColorItemBinding
import com.idunnololz.summit.databinding.SettingItemOnOffBinding
import com.idunnololz.summit.databinding.SettingItemOnOffMasterBinding
import com.idunnololz.summit.databinding.SettingSliderItemBinding
import com.idunnololz.summit.databinding.SettingTextValueBinding
import com.idunnololz.summit.databinding.TextOnlySettingItemBinding
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.ColorSettingItem
import com.idunnololz.summit.settings.OnOffSettingItem
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.settings.SliderSettingItem
import com.idunnololz.summit.settings.TextOnlySettingItem
import com.idunnololz.summit.settings.TextValueSettingItem
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.SummitActivity
import com.idunnololz.summit.util.colorPicker.OnColorPickedListener
import com.idunnololz.summit.util.colorPicker.utils.ColorPicker
import io.noties.markwon.Markwon

fun BasicSettingItem.bindTo(b: BasicSettingItemBinding, onValueChanged: () -> Unit) {
  if (this.icon == null) {
    b.icon.visibility = View.GONE
  } else {
    b.icon.setImageResource(this.icon)
    b.icon.visibility = View.VISIBLE
  }

  b.title.text = this.title

  if (this.description == null) {
    b.desc.visibility = View.GONE
  } else {
    b.desc.text = description
    b.desc.visibility = View.VISIBLE
  }

  b.root.setOnClickListener {
    onValueChanged()
  }
}

fun OnOffSettingItem.bindTo(
  b: SettingItemOnOffBinding,
  getCurrentValue: () -> Boolean,
  onValueChanged: (Boolean) -> Unit,
) {
  if (this.icon == null) {
    b.icon.visibility = View.GONE
  } else {
    b.icon.setImageResource(this.icon)
    b.icon.visibility = View.VISIBLE
  }

  b.title.text = this.title
  if (this.description != null) {
    b.desc.visibility = View.VISIBLE

    Markwon.create(b.root.context).setMarkdown(b.desc, description)
  } else {
    b.desc.visibility = View.GONE
  }

  // Unbind previous binding
  b.switchView.setOnCheckedChangeListener(null)
  b.switchView.isChecked = getCurrentValue()
  b.switchView.jumpDrawablesToCurrentState()
  b.switchView.setOnCheckedChangeListener { compoundButton, newValue ->
    onValueChanged(newValue)

    b.switchView.isChecked = getCurrentValue()
  }

  // Prevent auto state restoration since multiple checkboxes can have the same id
  b.switchView.isSaveEnabled = false
}

fun ColorSettingItem.bindTo(
  b: SettingColorItemBinding,
  globalStateStorage: GlobalStateStorage,
  getCurrentValue: () -> Int?,
  onValueChanged: (Int) -> Unit,
  defaultValue: (() -> Int),
) {
  val context = b.title.context

  if (this.icon == null) {
    b.icon.visibility = View.GONE
  } else {
    b.icon.setImageResource(this.icon)
    b.icon.visibility = View.VISIBLE
  }

  b.title.text = this.title

  if (this.description == null) {
    b.desc.visibility = View.GONE
  } else {
    b.desc.text = description
    b.desc.visibility = View.VISIBLE
  }

  b.colorInner.setBackgroundColor(
    getCurrentValue() ?: defaultValue(),
  )

  b.root.setOnClickListener {
    com.idunnololz.summit.util.colorPicker.ColorPickerDialog(
      context = context,
      title = title,
      color = getCurrentValue() ?: defaultValue(),
      defaultColor = defaultValue(),
      globalStateStorage = globalStateStorage,
    )
      .withAlphaEnabled(true)
      .withListener(object : OnColorPickedListener {
        override fun onColorPicked(pickerView: ColorPicker?, color: Int) {
          onValueChanged(color)
          b.colorInner.setBackgroundColor(getCurrentValue() ?: defaultValue())
        }
      })
      .show()
  }
}

var SettingTextValueBinding.isEnabled: Boolean
  get() = root.isEnabled
  set(value) {
    root.isEnabled = value
    this.title.isEnabled = value
    this.value.isEnabled = value
  }

var SettingItemOnOffBinding.isEnabled: Boolean
  get() = root.isEnabled
  set(value) {
    root.isEnabled = value
    this.title.isEnabled = value
    this.desc.isEnabled = value
    this.switchView.isEnabled = value
  }
