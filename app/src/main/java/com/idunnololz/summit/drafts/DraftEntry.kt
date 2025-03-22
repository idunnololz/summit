package com.idunnololz.summit.drafts

import android.os.Parcelable
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.util.crashLogger.crashLogger
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator

@Entity(tableName = "drafts")
@TypeConverters(DraftConverters::class)
@Parcelize
data class DraftEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "cts")
    val creationTs: Long,
    @ColumnInfo(name = "uts")
    val updatedTs: Long,
    @ColumnInfo(name = "draft_type")
    val draftType: Int,
    @ColumnInfo(name = "data")
    val data: DraftData?,
    @ColumnInfo(name = "account_id", defaultValue = "0")
    val accountId: Long,
    @ColumnInfo(name = "account_instance", defaultValue = "")
    val accountInstance: String,
) : Parcelable

@ProvidedTypeConverter
class DraftConverters(private val json: Json) {

    companion object {
        private const val TAG = "DraftConverters"
    }

    @TypeConverter
    fun draftDataToString(value: DraftData): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun stringToDraftData(value: String): DraftData? = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        Log.e(TAG, "", e)
        crashLogger?.recordException(e)
        null
    }
}

object DraftTypes {
    const val Post = 1
    const val Comment = 2
    const val Message = 3
}

@Serializable
@JsonClassDiscriminator("t")
sealed interface DraftData : Parcelable {
    val accountId: Long
    val accountInstance: String

    @Parcelize
    @Serializable
    @SerialName("1")
    data class PostDraftData(
        val originalPost: OriginalPostData?,
        val name: String?,
        val body: String?,
        val url: String?,
        val isNsfw: Boolean,
        override val accountId: Long,
        override val accountInstance: String,
        val targetCommunityFullName: String,
    ) : DraftData

    @Parcelize
    @Serializable
    @SerialName("2")
    data class CommentDraftData(
        val originalComment: OriginalCommentData?,
        val postRef: PostRef?,
        val parentCommentId: Int?,
        val content: String,
        override val accountId: Long,
        override val accountInstance: String,
    ) : DraftData

    @Parcelize
    @Serializable
    @SerialName("3")
    data class MessageDraftData(
        val targetAccountId: Long,
        val targetInstance: String,
        val content: String,
        override val accountId: Long,
        override val accountInstance: String,
    ) : DraftData
}

val DraftData.type
    get() = when (this) {
        is DraftData.CommentDraftData -> DraftTypes.Comment
        is DraftData.PostDraftData -> DraftTypes.Post
        is DraftData.MessageDraftData -> DraftTypes.Message
    }

@Parcelize
@Serializable
data class OriginalPostData(
    val name: String,
    val body: String?,
    val url: String?,
    val isNsfw: Boolean,
) : Parcelable

@Parcelize
@Serializable
data class OriginalCommentData(
    val postRef: PostRef,
    val commentId: Int,
    val content: String,
    val parentCommentId: Int?,
) : Parcelable
