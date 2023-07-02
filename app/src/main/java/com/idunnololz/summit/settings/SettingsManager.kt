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
            R.id.setting_theme,
            R.drawable.baseline_palette_24,
            context.getString(R.string.theme),
            context.getString(R.string.theme_settings_desc)
        ),
        BasicSettingItem(
            R.id.setting_view_type,
            R.drawable.baseline_view_agenda_black_24,
            context.getString(R.string.view_type),
            context.getString(R.string.view_type_settings_desc)
        ),
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

data class RadioGroupSettingItem(
    @IdRes override val id: Int,
    @DrawableRes val icon: Int,
    override val title: String,
    val description: String?,
    val options: List<RadioGroupOption>,
) : SettingItem {
    data class RadioGroupOption(
        @IdRes val id: Int,
        val title: String,
        val description: String?,
        @DrawableRes val icon: Int,
    )
}