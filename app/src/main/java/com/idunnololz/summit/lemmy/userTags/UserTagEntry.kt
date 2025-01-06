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
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.parcelize.Parcelize

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
@JsonClass(generateAdapter = true)
data class UserTagConfig(
    val tagName: String,
    val fillColor: Int,
    val borderColor: Int,
) : Parcelable

@ProvidedTypeConverter
class UserTagConverters(private val moshi: Moshi) {

    companion object {
        private const val TAG = "UserTagConverters"
    }

    @TypeConverter
    fun userTagConfigToString(value: UserTagConfig): String {
        return moshi.adapter<UserTagConfig>(UserTagConfig::class.java)
            .toJson(value)
    }

    @TypeConverter
    fun stringToUserTagConfig(value: String): UserTagConfig? = try {
        moshi.adapter<UserTagConfig>(UserTagConfig::class.java)
            .fromJson(value)
    } catch (e: Exception) {
        Log.e(TAG, "", e)
        crashlytics?.recordException(e)
        null
    }
}
