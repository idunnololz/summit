package com.idunnololz.summit.settings

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import kotlinx.parcelize.Parcelize

var nextId = 1

sealed class SettingItem : Parcelable {
    open val id: Int = nextId++
    abstract val title: String
    abstract val description: String?
    open val isEnabled: Boolean = true
    open val relatedKeys: List<String> = listOf()
}

@Parcelize
data class SubgroupItem(
    override val title: String,
    val settings: List<SettingItem>,
    override val relatedKeys: List<String> = listOf(),
) : SettingItem() {
    override val description: String? = null
}

@Parcelize
data class BasicSettingItem(
    @DrawableRes val icon: Int?,
    override val title: String,
    override val description: String?,
    override val relatedKeys: List<String> = listOf(),
) : SettingItem()

@Parcelize
data class TextOnlySettingItem(
    override val title: String,
    override val description: String?,
    override val relatedKeys: List<String> = listOf(),
) : SettingItem()

/**
 * This is not a real setting item. Its purpose is to describe a setting pager.
 */
@Parcelize
data class DescriptionSettingItem(
    override val title: String,
    override val description: String?,
    override val relatedKeys: List<String> = listOf(),
) : SettingItem()

@Parcelize
data class TextValueSettingItem(
    override val title: String,
    override val description: String?,
    val supportsRichText: Boolean,
    override val isEnabled: Boolean = true,
    val hint: String? = null,
    override val relatedKeys: List<String> = listOf(),
) : SettingItem()

@Parcelize
data class SliderSettingItem(
    override val title: String,
    val minValue: Float,
    val maxValue: Float,
    val stepSize: Float? = null,
    override val relatedKeys: List<String> = listOf(),
) : SettingItem() {
    override val description: String? = null
}

@Parcelize
data class OnOffSettingItem(
    @DrawableRes val icon: Int?,
    override val title: String,
    override val description: String?,
    override val relatedKeys: List<String> = listOf(),
) : SettingItem()

@Parcelize
data class ColorSettingItem(
    @DrawableRes val icon: Int?,
    override val title: String,
    override val description: String?,
    override val relatedKeys: List<String> = listOf(),
) : SettingItem()

@Parcelize
data class RadioGroupSettingItem(
    @DrawableRes val icon: Int?,
    override val title: String,
    override val description: String?,
    val options: List<RadioGroupOption>,
    override val relatedKeys: List<String> = listOf(),
) : SettingItem() {
    @Parcelize
    data class RadioGroupOption(
        @IdRes val id: Int,
        val title: String,
        val description: String?,
        @DrawableRes val icon: Int?,
        val isDefault: Boolean = false,
    ) : Parcelable
}

@Parcelize
data class ImageValueSettingItem(
    override val title: String,
    override val description: String?,
    val isSquare: Boolean,
    override val relatedKeys: List<String> = listOf(),
) : SettingItem()
