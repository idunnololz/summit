package com.idunnololz.summit.settings

import android.content.Context
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.google.android.material.slider.Slider
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.OnOffSettingItemBinding
import com.idunnololz.summit.databinding.SliderSettingItemBinding
import com.idunnololz.summit.databinding.TextOnlySettingItemBinding
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.util.BottomMenu
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val mainSettings = listOf(
        BasicSettingItem(
            R.id.setting_view_type,
            R.drawable.baseline_view_agenda_black_24,
            context.getString(R.string.view_type),
            context.getString(R.string.view_type_settings_desc)
        )
    )

    fun getSettingsForMainPage() =
        mainSettings
}


sealed interface SettingItem {
    val id: Int
    val title: String
}
data class BasicSettingItem(
    @IdRes override val id: Int,
    @DrawableRes val icon: Int,
    override val title: String,
    val description: String?,
) : SettingItem

data class TextOnlySettingItem(
    @IdRes override val id: Int,
    override val title: String,
    val description: String?,
) : SettingItem

data class SliderSettingItem(
    @IdRes override val id: Int,
    override val title: String,
    val minValue: Float,
    val maxValue: Float,
) : SettingItem

data class OnOffSettingItem(
    @IdRes override val id: Int,
    override val title: String,
    val description: String?,
    val isOn: Boolean,
) : SettingItem

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

    b.switchView.setOnCheckedChangeListener { compoundButton, b ->
        onValueChanged(b)
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