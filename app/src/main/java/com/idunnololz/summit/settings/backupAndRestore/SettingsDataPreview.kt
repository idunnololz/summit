package com.idunnololz.summit.settings.backupAndRestore

import android.os.Parcelable
import com.idunnololz.summit.settings.SettingItem
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SettingsDataPreview(
    val keys: List<String>,
    val keyToSettingItems: Map<String, List<SettingItem>>,
    val diffs: List<Diff>,
    val settingsPreview: Map<String, String>,
    val keyToType: Map<String, String>,
    val rawData: String,
) : Parcelable

@Parcelize
data class Diff(
    val type: DiffType,
    val currentValue: String,
    val importValue: String,
) : Parcelable

@Parcelize
data class SettingsData(
    val rawData: String,
    val excludeKeys: Set<String>,
) : Parcelable

enum class DiffType {
    Added,
    Removed,
    Changed,
    Unchanged,
}
