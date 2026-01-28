package com.idunnololz.summit.localTracking.community

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.util.crashLogger.crashLogger
import kotlinx.serialization.json.Json

@Entity(tableName = "community_tracker")
data class CommunityStatEntry(
  @PrimaryKey(autoGenerate = true)
  val id: Long,
  val userId: Long,
  val lastUpdateTs: Long,
  val createdTs: Long,
  val communityRef: CommunityRef,
  val icon: String?,
  val hits: Int,
  val lastMetadataUpdateTs: Long,
  val mau: Int? = null,
)

@ProvidedTypeConverter
class CommunityStatEntryConverters(
  private val json: Json,
) {

  companion object {
    private const val TAG = "CommunityStatEntryConverters"
  }

  @TypeConverter
  fun communityRefToString(value: CommunityRef): String = json.encodeToString(value)

  @TypeConverter
  fun stringToCommunityRef(value: String): CommunityRef? = try {
    json.decodeFromString(value)
  } catch (e: Exception) {
    Log.e(TAG, "", e)
    crashLogger?.recordException(e)
    null
  }
}