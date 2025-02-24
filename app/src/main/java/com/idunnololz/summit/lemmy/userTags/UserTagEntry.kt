package com.idunnololz.summit.lemmy.userTags

import android.os.Parcelable
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.idunnololz.summit.util.crashlytics
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity(tableName = "user_tags")
@Parcelize
@TypeConverters(UserTagConverters::class)
data class UserTagEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "actor_id")
    val actorId: String,
    @ColumnInfo(name = "tag")
    val tag: UserTagConfig,
    @ColumnInfo(name = "create_ts")
    val createTs: Long,
    @ColumnInfo(name = "update_ts")
    val updateTs: Long,
) : Parcelable

@Parcelize
@Serializable
data class UserTagConfig(
    val tagName: String,
    val fillColor: Int,
    val borderColor: Int,
) : Parcelable

@ProvidedTypeConverter
class UserTagConverters(private val json: Json) {

    companion object {
        private const val TAG = "UserTagConverters"
    }

    @TypeConverter
    fun userTagConfigToString(value: UserTagConfig): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun stringToUserTagConfig(value: String): UserTagConfig? = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        Log.e(TAG, "", e)
        crashlytics?.recordException(e)
        null
    }
}
