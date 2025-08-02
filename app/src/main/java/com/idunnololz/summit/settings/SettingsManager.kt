package com.idunnololz.summit.settings

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import kotlinx.parcelize.Parcelize

var nextId = 1

sealed class SettingItem : Parcelable {
  abstract val id: Int
  abstract val title: String
  abstract val description: String?
  open val isEnabled: Boolean = true
  open val relatedKeys: List<String> = listOf()
}

@Parcelize
data class SubgroupItem(
  override val title: String,
  override val description: String?,
  val settings: List<SettingItem>,
  override val relatedKeys: List<String> = listOf(),
  override val id: Int = nextId++,
) : SettingItem()

@Parcelize
data class BasicSettingItem(
  @DrawableRes val icon: Int?,
  override val title: String,
  override val description: String?,
  override val relatedKeys: List<String> = listOf(),
  override val id: Int = nextId++,
) : SettingItem()

@Parcelize
data class TextOnlySettingItem(
  override val title: String,
  override val description: String?,
  override val relatedKeys: List<String> = listOf(),
  override val id: Int = nextId++,
) : SettingItem()

/**
 * This is not a real setting item. Its purpose is to describe a setting pager.
 */
@Parcelize
data class DescriptionSettingItem(
  override val title: String,
  override val description: String?,
  override val relatedKeys: List<String> = listOf(),
  override val id: Int = nextId++,
) : SettingItem()

@Parcelize
data class TextValueSettingItem(
  override val title: String,
  override val description: String?,
  val supportsRichText: Boolean,
  override val isEnabled: Boolean = true,
  val hint: String? = null,
  override val relatedKeys: List<String> = listOf(),
  override val id: Int = nextId++,
) : SettingItem()

@Parcelize
data class SliderSettingItem(
  override val title: String,
  val minValue: Float,
  val maxValue: Float,
  val stepSize: Float? = null,
  override val relatedKeys: List<String> = listOf(),
  override val id: Int = nextId++,
) : SettingItem() {
  override val description: String? = null
}

@Parcelize
data class OnOffSettingItem(
  @DrawableRes val icon: Int?,
  override val title: String,
  override val description: String?,
  override val relatedKeys: List<String> = listOf(),
  override val id: Int = nextId++,
) : SettingItem()

@Parcelize
data class ColorSettingItem(
  @DrawableRes val icon: Int?,
  override val title: String,
  override val description: String?,
  override val relatedKeys: List<String> = listOf(),
  override val id: Int = nextId++,
) : SettingItem()

@Parcelize
data class RadioGroupSettingItem(
  @DrawableRes val icon: Int?,
  override val title: String,
  override val description: String?,
  val options: List<RadioGroupOption>,
  override val isEnabled: Boolean = true,
  override val relatedKeys: List<String> = listOf(),
  override val id: Int = nextId++,
) : SettingItem() {
  @Parcelize
  data class RadioGroupOption(
    @IdRes val id: Int,
    val title: String,
    val description: String? = null,
    @DrawableRes val icon: Int? = null,
    val isDefault: Boolean = false,
  ) : Parcelable
}

@Parcelize
data class ImageValueSettingItem(
  override val title: String,
  override val description: String?,
  val isSquare: Boolean,
  override val relatedKeys: List<String> = listOf(),
  override val id: Int = nextId++,
) : SettingItem()
