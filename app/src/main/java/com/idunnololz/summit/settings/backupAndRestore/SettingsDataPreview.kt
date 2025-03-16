package com.idunnololz.summit.settings.backupAndRestore

import android.os.Parcelable
import com.idunnololz.summit.db.raw.TableInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class SettingsDataPreview(
    val keys: List<String>,
    val diffs: List<Diff>,
    val settingsPreview: Map<String, String>,
    val keyToType: Map<String, String>,
    val rawData: String,
    val tablePath: String?,
    val databaseTablePreview: Map<String, TableInfo>
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
    val tablePath: String?,
    val excludeKeys: Set<String>,
    val tableResolutions: Map<String, SettingDataAdapter.ImportTableResolution>,
) : Parcelable

@Parcelize
data class TableSummary(
    val tableName: String,
    val rows: Int,
) : Parcelable

enum class DiffType {
    Added,
    Removed,
    Changed,
    Unchanged,
}
