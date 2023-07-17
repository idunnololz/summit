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

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mainSettings: MainSettings,
) {

    private val mainSettingItems = listOf(
        SubgroupItem(
            context.getString(R.string.appearance),
            listOf(
                mainSettings.settingTheme,
                mainSettings.settingContent,
                mainSettings.settingViewType,
                mainSettings.settingPostAndComment,
            )
        ),
        SubgroupItem(
            context.getString(R.string.behavior),
            listOf(
                mainSettings.settingLemmyWeb,
                mainSettings.settingGestures,
                mainSettings.settingHistory,
            )
        ),
        SubgroupItem(
            context.getString(R.string.systems),
            listOf(
                mainSettings.settingCache,
                mainSettings.settingHiddenPosts,
            )
        )
    )

    fun getSettingsForMainPage() =
        mainSettingItems
}

var nextId = 1

sealed class SettingItem : Parcelable {
    val id: Int = nextId++
    abstract val title: String
    open val isEnabled: Boolean = true
}

@Parcelize
data class SubgroupItem(
    override val title: String,
    val settings: List<SettingItem>,
) : SettingItem()

@Parcelize
data class BasicSettingItem(
    @DrawableRes val icon: Int?,
    override val title: String,
    val description: String?,
) : SettingItem()

@Parcelize
data class TextOnlySettingItem(
    override val title: String,
    val description: String?,
) : SettingItem()

@Parcelize
data class TextValueSettingItem(
    override val title: String,
    val supportsRichText: Boolean,
    override val isEnabled: Boolean = true,
    val hint: String? = null,
) : SettingItem()

@Parcelize
data class SliderSettingItem(
    override val title: String,
    val minValue: Float,
    val maxValue: Float,
) : SettingItem()

@Parcelize
data class OnOffSettingItem(
    override val title: String,
    val description: String?,
) : SettingItem()

@Parcelize
data class RadioGroupSettingItem(
    @DrawableRes val icon: Int?,
    override val title: String,
    val description: String?,
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
    val description: String?,
    val isSquare: Boolean,
) : SettingItem()