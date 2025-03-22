package com.idunnololz.summit.lemmy.inbox

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.idunnololz.summit.util.crashLogger.crashLogger
import kotlinx.serialization.json.Json

@Entity(tableName = "inbox_entries")
@TypeConverters(InboxEntryConverters::class)
data class InboxEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "ts")
    val ts: Long,
    @ColumnInfo(name = "item_id")
    val itemId: Int,
    @ColumnInfo(name = "notification_id")
    val notificationId: Int,
    @ColumnInfo(name = "account_full_name")
    val accountFullName: String,
    @ColumnInfo(name = "inbox_item")
    val inboxItem: InboxItem?,
)

@ProvidedTypeConverter
class InboxEntryConverters(private val json: Json) {

    companion object {
        private const val TAG = "InboxEntryConverters"
    }

    @TypeConverter
    fun inboxItemToString(value: InboxItem): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun stringToInboxItem(value: String): InboxItem? = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        Log.e(TAG, "", e)
        crashLogger?.recordException(e)
        null
    }
}
