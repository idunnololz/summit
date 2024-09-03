package com.idunnololz.summit.actions.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.idunnololz.summit.drafts.DraftConverters
import com.idunnololz.summit.drafts.DraftData
import kotlinx.parcelize.Parcelize

@Entity(tableName = "read_posts")
@Parcelize
class ReadPostEntry(
    @PrimaryKey
    @ColumnInfo(name = "post_key")
    val postKey: String,
    @ColumnInfo(name = "read")
    val read: Boolean,
) : Parcelable