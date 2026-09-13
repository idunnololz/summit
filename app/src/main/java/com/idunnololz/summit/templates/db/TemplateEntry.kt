package com.idunnololz.summit.templates.db

import android.os.Parcelable
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.idunnololz.summit.api.dto.lemmy.LanguageId
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.drafts.OriginalCommentData
import com.idunnololz.summit.drafts.OriginalPostData
import com.idunnololz.summit.lemmy.PostRef
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
class TemplateConverters(
  private val json: Json,
) {

  companion object {
    private const val TAG = "TemplateConverters"
  }

  @TypeConverter
  fun templateDataToString(value: TemplateData): String = json.encodeToString(value)

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
  const val Post = 2
  const val Comment = 2
}

val TemplateData.type
  get() = when (this) {
    is TemplateData.RegistrationApplicationRejectionTemplateData ->
      TemplateTypes.RegistrationApplicationRejection
    is TemplateData.CommentTemplateData ->
      TemplateTypes.Comment
    is TemplateData.PostTemplateData ->
      TemplateTypes.Post
  }

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("t")
sealed interface TemplateData : Parcelable {
  val accountId: Long
  val accountInstance: String
  val title: String
  val content: String

  @Parcelize
  @Serializable
  @SerialName("1")
  data class RegistrationApplicationRejectionTemplateData(
    override val content: String,
    override val accountId: Long,
    override val title: String,
    override val accountInstance: String,
  ) : TemplateData


  @Parcelize
  @Serializable
  @SerialName("2")
  data class PostTemplateData(
    val url: String?,
    val isNsfw: Boolean,
    override val title: String,
    override val content: String,
    override val accountId: Long,
    override val accountInstance: String,
    val targetCommunityFullName: String,
    val thumbnailUrl: String? = null,
    val altText: String? = null,
    val languageId: LanguageId? = null,
  ) : TemplateData

  @Parcelize
  @Serializable
  @SerialName("3")
  data class CommentTemplateData(
    val originalComment: OriginalCommentData?,
    val postRef: PostRef?,
    val parentCommentId: Int?,
    override val title: String,
    override val content: String,
    override val accountId: Long,
    override val accountInstance: String,
    val languageId: LanguageId? = null,
  ) : TemplateData
}
