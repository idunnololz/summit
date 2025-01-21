package com.idunnololz.summit.settings.util

import android.view.View
import com.google.android.material.slider.Slider
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.BasicSettingItemBinding
import com.idunnololz.summit.databinding.RadioGroupOptionSettingItemBinding
import com.idunnololz.summit.databinding.RadioGroupTitleSettingItemBinding
import com.idunnololz.summit.databinding.SettingColorItemBinding
import com.idunnololz.summit.databinding.SettingItemOnOffBinding
import com.idunnololz.summit.databinding.SettingItemOnOffMasterBinding
import com.idunnololz.summit.databinding.SettingTextValueBinding
import com.idunnololz.summit.databinding.SliderSettingItemBinding
import com.idunnololz.summit.databinding.TextOnlySettingItemBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.ColorSettingItem
import com.idunnololz.summit.settings.OnOffSettingItem
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.settings.SliderSettingItem
import com.idunnololz.summit.settings.TextOnlySettingItem
import com.idunnololz.summit.settings.TextValueSettingItem
import com.idunnololz.summit.util.BottomMenu
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

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

        b.desc.text = LemmyTextHelper.getSpannable(b.root.context, description)
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

fun OnOffSettingItem.bindTo(
    b: SettingItemOnOffMasterBinding,
    getCurrentValue: () -> Boolean,
    onValueChanged: (Boolean) -> Unit,
) {
    b.title.text = this.title
    if (this.description != null) {
        b.desc.visibility = View.VISIBLE

        b.desc.text = LemmyTextHelper.getSpannable(b.root.context, description)
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
    b.card.setOnClickListener {
        b.switchView.performClick()
    }

    // Prevent auto state restoration since multiple checkboxes can have the same id
    b.switchView.isSaveEnabled = false
}

fun SliderSettingItem.bindTo(
    b: SliderSettingItemBinding,
    getCurrentValue: () -> Float,
    onValueChanged: (Float) -> Unit,
) {
    b.title.text = this.title
    b.slider.valueFrom = this.minValue
    b.slider.valueTo = this.maxValue

    val stepSize = stepSize
    if (stepSize != null) {
        b.slider.stepSize = stepSize
        b.slider.value =
            ((getCurrentValue() / stepSize).toInt() * b.slider.stepSize)
                .coerceIn(minValue, maxValue)
    } else {
        b.slider.value = getCurrentValue().coerceIn(minValue, maxValue)
    }

    b.slider.addOnSliderTouchListener(
        object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                onValueChanged(slider.value)
            }
        },
    )

    // Prevent auto state restoration since multiple checkboxes can have the same id
    b.slider.isSaveEnabled = false
}

fun <T> TextOnlySettingItem.bindTo(
    activity: MainActivity,
    b: TextOnlySettingItemBinding,
    choices: Map<T, String>,
    getCurrentChoice: () -> T,
    onChoiceSelected: (T) -> Unit,
) {
    b.title.text = this.title
    b.desc.text = choices[getCurrentChoice()]

    b.root.setOnClickListener {
        val curChoice = getCurrentChoice()
        val bottomMenu = BottomMenu(b.root.context)
            .apply {
                val idToChoice = mutableMapOf<Int, T>()
                choices.entries.withIndex().forEach { (index, entry) ->
                    idToChoice[index] = entry.key
                    addItem(index, entry.value)

                    if (getCurrentChoice() == entry.key) {
                        setChecked(index)
                    }
                }

                setTitle(title)

                setOnMenuItemClickListener {
                    onChoiceSelected(requireNotNull(idToChoice[it.id]))
                    b.desc.text = choices[getCurrentChoice()]
                }
            }
        activity.showBottomMenu(bottomMenu)
    }
}

fun RadioGroupSettingItem.bindTo(
    b: SettingTextValueBinding,
    getCurrentValue: () -> Int,
    onSettingClick: (setting: RadioGroupSettingItem, currentValue: Int) -> Unit,
) {
    b.title.text = this.title

    if (this.description == null) {
        b.desc.visibility = View.GONE
    } else {
        b.desc.text = this.description
        b.desc.visibility = View.VISIBLE
    }

    b.value.text = this.options.firstOrNull { it.id == getCurrentValue() }?.title

    b.root.tag = this
    b.root.setOnClickListener {
        onSettingClick(this, getCurrentValue())
    }
}

fun RadioGroupSettingItem.bindToMultiView(
    b: RadioGroupTitleSettingItemBinding,
    optionsB: List<RadioGroupOptionSettingItemBinding>,
    getCurrentValue: () -> Int,
    onValueChanged: (Int) -> Unit,
) {
    require(optionsB.size == this.options.size)

    b.title.text = this.title

    val zipped = this.options.zip(optionsB)

    fun updateChecked() {
        val currentValue = getCurrentValue()
        zipped.forEach { (option, b) ->
            b.radioButton.isChecked = option.id == currentValue
        }
    }
    updateChecked()

    zipped.forEach { (option, b) ->
        b.title.text = option.title
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
            if (value && getCurrentValue() != option.id) {
                onValueChanged(option.id)
            }
            updateChecked()
        }
    }
}

fun ColorSettingItem.bindTo(
    b: SettingColorItemBinding,
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
        ColorPickerDialog.Builder(context)
            .setTitle(title)
            .setPositiveButton(
                context.getString(android.R.string.ok),
                ColorEnvelopeListener { envelope, _ ->
                    onValueChanged(envelope.color)

                    b.colorInner.setBackgroundColor(getCurrentValue() ?: defaultValue())
                },
            )
            .setNegativeButton(
                context.getString(android.R.string.cancel),
            ) { dialogInterface, i -> dialogInterface.dismiss() }
            .attachAlphaSlideBar(true) // the default value is true.
            .attachBrightnessSlideBar(true) // the default value is true.
            .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
            .apply {
                if (defaultValue != null) {
                    setNeutralButton(
                        context.getString(R.string.reset_to_default),
                    ) { dialogInterface, i ->
                        dialogInterface.dismiss()

                        onValueChanged(defaultValue())

                        b.colorInner.setBackgroundColor(getCurrentValue() ?: defaultValue())
                    }
                }
            }
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

fun TextValueSettingItem.bindTo(
    b: SettingTextValueBinding,
    getCurrentValue: () -> String,
    onSettingClick: (setting: TextValueSettingItem, currentValue: String) -> Unit,
) {
    b.title.text = this.title

    if (this.description == null) {
        b.desc.visibility = View.GONE
    } else {
        b.desc.text = this.description
        b.desc.visibility = View.VISIBLE
    }

    b.value.text = getCurrentValue()

    b.root.tag = this
    b.root.setOnClickListener {
        onSettingClick(this, getCurrentValue())
    }
}
