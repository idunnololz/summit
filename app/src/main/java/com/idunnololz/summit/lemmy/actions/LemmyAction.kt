package com.idunnololz.summit.lemmy.actions

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

sealed interface LemmyAction {
    val id: Long
    val ts: Long
    val creationTs: Long
    val info: ActionInfo?
}