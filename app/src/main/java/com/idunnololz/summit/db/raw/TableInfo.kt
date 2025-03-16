package com.idunnololz.summit.db.raw

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TableInfo(
    val tableName: String,
    val rowCount: Int,
) : Parcelable