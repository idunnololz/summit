package com.idunnololz.summit.db.raw

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ColumnInfo(
    val columnName: String,
    val type: String,
    val notNull: Boolean,
    val defaultValue: String?,
    val primaryKey: Boolean,
    val isSensitive: Boolean,
) : Parcelable