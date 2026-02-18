package com.idunnololz.summit.filterLists

import android.os.Parcelable
import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.idunnololz.summit.util.crashLogger.crashLogger
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity(tableName = "content_filters")
@Parcelize
@TypeConverters(FilterEntryConverters::class)
data class FilterEntry(
  @PrimaryKey(autoGenerate = true)
  val id: Long,
  /**
   * What this filter is for. Eg. post list, comments, etc.
   */
  val contentType: ContentTypeId,
  /**
   * What this filter is against. Eg. post title, instance, etc.
   */
  val filterType: FilterTypeId,
  val filter: String,
  val isRegex: Boolean,
  val options: FilterEntryOptions? = null,
) : Parcelable

@ProvidedTypeConverter
class FilterEntryConverters(
  private val json: Json,
) {

  companion object {
    private const val TAG = "FilterEntryConverters"
  }

  @TypeConverter
  fun filterEntryOptionsToString(value: FilterEntryOptions): String = json.encodeToString(value)

  @TypeConverter
  fun stringToFilterEntryOptions(value: String): FilterEntryOptions? = try {
    json.decodeFromString(value)
  } catch (e: Exception) {
    Log.e(TAG, "", e)
    crashLogger?.recordException(e)
    null
  }
}

@Parcelize
@Serializable
data class FilterEntryOptions(
  val matchWholeWord: Boolean? = null,
) : Parcelable

typealias FilterTypeId = Int

object FilterTypeIds {
  const val KeywordFilter = 1
  const val InstanceFilter = 2
  const val CommunityFilter = 3
  const val UserFilter = 4
  const val UrlFilter = 5
}

typealias ContentTypeId = Int

object ContentTypeIds {
  const val PostListType = 1
  const val CommentListType = 2
}
