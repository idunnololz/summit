package com.idunnololz.summit.settings

import android.content.Context
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.idunnololz.summit.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

var nextId = 1

sealed class SettingItem : Parcelable {
    val id: Int = nextId++
    abstract val title: String
    abstract val description: String?
    open val isEnabled: Boolean = true
}

@Parcelize
data class SubgroupItem(
    override val title: String,
    val settings: List<SettingItem>,
) : SettingItem() {
    override val description: String? = null
}

@Parcelize
data class BasicSettingItem(
    @DrawableRes val icon: Int?,
    override val title: String,
    override val description: String?,
) : SettingItem()

@Parcelize
data class TextOnlySettingItem(
    override val title: String,
    override val description: String?,
) : SettingItem()

@Parcelize
data class TextValueSettingItem(
    override val title: String,
    val supportsRichText: Boolean,
    override val isEnabled: Boolean = true,
    val hint: String? = null,
) : SettingItem() {
    override val description: String? = null
}

@Parcelize
data class SliderSettingItem(
    override val title: String,
    val minValue: Float,
    val maxValue: Float,
    val stepSize: Float? = null,
) : SettingItem() {
    override val description: String? = null
}

@Parcelize
data class OnOffSettingItem(
    @DrawableRes val icon: Int?,
    override val title: String,
    override val description: String?,
) : SettingItem()

@Parcelize
data class RadioGroupSettingItem(
    @DrawableRes val icon: Int?,
    override val title: String,
    override val description: String?,
    val options: List<RadioGroupOption>,
) : SettingItem() {
    @Parcelize
    data class RadioGroupOption(
        @IdRes val id: Int,
        val title: String,
        val description: String?,
        @DrawableRes val icon: Int?,
    ) : Parcelable
}

@Parcelize
data class ImageValueSettingItem(
    override val title: String,
    override val description: String?,
    val isSquare: Boolean,
) : SettingItem()
