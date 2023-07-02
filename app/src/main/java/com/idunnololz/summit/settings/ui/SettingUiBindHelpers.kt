package com.idunnololz.summit.settings.ui

import android.util.Log
import android.view.View
import com.google.android.material.slider.Slider
import com.idunnololz.summit.databinding.OnOffSettingItemBinding
import com.idunnololz.summit.databinding.RadioGroupOptionSettingItemBinding
import com.idunnololz.summit.databinding.RadioGroupTitleSettingItemBinding
import com.idunnololz.summit.databinding.SliderSettingItemBinding
import com.idunnololz.summit.databinding.TextOnlySettingItemBinding
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.settings.OnOffSettingItem
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.settings.SliderSettingItem
import com.idunnololz.summit.settings.TextOnlySettingItem
import com.idunnololz.summit.util.BottomMenu


fun OnOffSettingItem.bindTo(
    b: OnOffSettingItemBinding,
    getCurrentValue: () -> Boolean,
    onValueChanged: (Boolean) -> Unit,
) {
    b.title.text = this.title
    if (this.description != null) {
        b.desc.visibility = View.VISIBLE
        b.desc.text = this.description
    } else {
        b.desc.visibility = View.GONE
    }
    b.switchView.isChecked = getCurrentValue()

    b.switchView.setOnCheckedChangeListener { compoundButton, newValue ->
        onValueChanged(newValue)

        b.switchView.isChecked = getCurrentValue()
    }
}

fun SliderSettingItem.bindTo(
    b: SliderSettingItemBinding,
    getCurrentValue: () -> Float,
    onValueChanged: (Float) -> Unit,
) {
    b.title.text = this.title
    b.slider.valueFrom = this.minValue
    b.slider.valueTo = this.maxValue

    b.slider.value = getCurrentValue()

    b.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {}

        override fun onStopTrackingTouch(slider: Slider) {
            onValueChanged(slider.value)
        }
    })
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
        b.title.setCompoundDrawablesRelativeWithIntrinsicBounds(option.icon, 0, 0, 0)

        if (option.description != null) {
            b.desc.visibility = View.VISIBLE
            b.desc.text = option.description
        } else {
            b.desc.visibility = View.GONE
        }

        b.radioButton.setOnCheckedChangeListener { compoundButton, b ->
            Log.d("HAHA", "getCurrentValue(): ${getCurrentValue()}, id: ${option.id}")
            if (b && getCurrentValue() != option.id) {
                onValueChanged(option.id)
            }
            updateChecked()
        }
    }
}