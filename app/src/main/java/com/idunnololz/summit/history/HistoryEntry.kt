package com.idunnololz.summit.history

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val type: Int,
    val reason: HistorySaveReason,
    val url: String,
    val shortDesc: String,
    val ts: Long,
    val extras: String,
) {
    companion object {
        const val TYPE_PAGE_VISIT = 1
        const val TYPE_COMMUNITY_STATE = 2
    }
}

data class LiteHistoryEntry(
    val id: Long,
    val type: Int,
    val reason: HistorySaveReason,
    val url: String,
    val shortDesc: String,
    val ts: Long,
)

/**
 * Reason why a history entry was saved.
 */
enum class HistorySaveReason {
    UNKNOWN,
    LOADING,
    LOADED,
    LEAVE_SCREEN,
    CLOSE,
}

class HistoryConverters {
    @TypeConverter
    fun fromReasonInt(value: Int?): HistorySaveReason? =
        when (value) {
            0 -> HistorySaveReason.UNKNOWN
            1 -> HistorySaveReason.LOADING
            2 -> HistorySaveReason.LOADED
            3 -> HistorySaveReason.LEAVE_SCREEN
            4 -> HistorySaveReason.CLOSE
            else -> HistorySaveReason.UNKNOWN
        }

    @TypeConverter
    fun reasonToInt(date: HistorySaveReason?): Int =
        when (date) {
            HistorySaveReason.UNKNOWN -> 0
            HistorySaveReason.LOADING -> 1
            HistorySaveReason.LOADED -> 2
            HistorySaveReason.LEAVE_SCREEN -> 3
            HistorySaveReason.CLOSE -> 4
            null -> 0
        }
}
