package com.idunnololz.summit.emoji.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "text_emojis")
@Parcelize
data class TextEmojiEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "created_ts")
    val createdTs: Long,
    @ColumnInfo(name = "modified_ts")
    val modifiedTs: Long,
    @ColumnInfo(name = "read")
    val emoji: String,
    @ColumnInfo(name = "order")
    val order: Int,
    @ColumnInfo(name = "modifiable")
    val modifiable: Boolean,
) : Parcelable
