package com.idunnololz.summit.templates.db

import android.os.Parcelable
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.idunnololz.summit.drafts.DraftConverters
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.drafts.DraftTypes
import com.idunnololz.summit.util.crashLogger.crashLogger
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator

@Entity(tableName = "templates")
@TypeConverters(TemplateConverters::class)
@Parcelize
data class TemplateEntry(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Long,
  @ColumnInfo(name = "cts")
  val creationTs: Long,
  @ColumnInfo(name = "uts")
  val updatedTs: Long,
  @ColumnInfo(name = "account_id")
  val accountId: Long,
  @ColumnInfo(name = "account_instance")
  val accountInstance: String,
  @ColumnInfo(name = "template_type")
  val templateType: Int,
  @ColumnInfo(name = "data")
  val data: TemplateData?,
) : Parcelable

@ProvidedTypeConverter
class TemplateConverters(private val json: Json) {

  companion object {
    private const val TAG = "TemplateConverters"
  }

  @TypeConverter
  fun templateDataToString(value: TemplateData): String {
    return json.encodeToString(value)
  }

  @TypeConverter
  fun stringToTemplateData(value: String): TemplateData? = try {
    json.decodeFromString(value)
  } catch (e: Exception) {
    Log.e(TAG, "", e)
    crashLogger?.recordException(e)
    null
  }
}

object TemplateTypes {
  const val RegistrationApplicationRejection = 1
}

val TemplateData.type
  get() = when (this) {
    is TemplateData.RegistrationApplicationRejectionTemplateData ->
      TemplateTypes.RegistrationApplicationRejection
  }

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("t")
sealed interface TemplateData : Parcelable {
  val accountId: Long
  val accountInstance: String
  val content: String

  @Parcelize
  @Serializable
  @SerialName("1")
  data class RegistrationApplicationRejectionTemplateData(
    override val content: String,
    override val accountId: Long,
    override val accountInstance: String,
  ) : TemplateData
}
