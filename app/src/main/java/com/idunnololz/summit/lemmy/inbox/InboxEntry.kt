package com.idunnololz.summit.lemmy.inbox

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi

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
class InboxEntryConverters(private val moshi: Moshi) {

    companion object {
        private const val TAG = "InboxEntryConverters"
    }

    @TypeConverter
    fun inboxItemToString(value: InboxItem): String {
        return moshi.adapter(InboxItem::class.java).toJson(value)
    }

    @TypeConverter
    fun stringToInboxItem(value: String): InboxItem? = try {
        moshi.adapter(InboxItem::class.java).fromJson(value)
    } catch (e: Exception) {
        Log.e(TAG, "", e)
        FirebaseCrashlytics.getInstance().recordException(e)
        null
    }
}
