package com.idunnololz.summit.lemmy.actions

import android.os.Parcelable
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import arrow.core.Either
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.util.crashLogger.crashLogger
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator

@Dao
interface LemmyActionsDao {

    @Query("SELECT * FROM lemmy_actions")
    suspend fun getAllPendingActions(): List<LemmyPendingAction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: LemmyPendingAction): Long

    @Delete
    suspend fun delete(action: LemmyPendingAction)

    @Query("DELETE FROM lemmy_actions")
    suspend fun deleteAllActions()

    @Query("SELECT COUNT(*) FROM lemmy_actions")
    suspend fun count(): Int
}

@Entity(tableName = "lemmy_actions")
@TypeConverters(LemmyActionConverters::class)
data class LemmyPendingAction(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    override val id: Long,
    @ColumnInfo(name = "ts")
    override val ts: Long,
    @ColumnInfo(name = "cts")
    override val creationTs: Long,
    @ColumnInfo(name = "info")
    override val info: ActionInfo?,
) : LemmyAction

@ProvidedTypeConverter
class LemmyActionConverters(private val json: Json) {

    companion object {
        private const val TAG = "LemmyActionConverters"
    }

    @TypeConverter
    fun actionInfoToString(value: ActionInfo): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun stringToActionInfo(value: String): ActionInfo? = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        Log.e(TAG, "", e)
        crashLogger?.recordException(e)
        null
    }

    @TypeConverter
    fun lemmyActionFailureReasonToString(value: LemmyActionFailureReason): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun stringToLemmyActionFailureReason(value: String): LemmyActionFailureReason? = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        Log.e(TAG, "", e)
        crashLogger?.recordException(e)
        null
    }
}

@Serializable
@JsonClassDiscriminator("t")
sealed interface ActionInfo : Parcelable {

    val accountId: Long?
    val accountInstance: String?
    val action: ActionType
    val isAffectedByRateLimit: Boolean
    val retries: Int

    @Parcelize
    @Serializable
    @SerialName("1")
    data class VoteActionInfo(
        /**
         * Instance where the object lives.
         */
        val instance: String,
        /**
         * What to vote on
         */
        val ref: VotableRef,
        /**
         * -1, 0 or 1
         */
        val dir: Int,
        val rank: Int,
        override val accountId: Long,
        override val accountInstance: String?,
        override val retries: Int = 0,
        override val action: ActionType = ActionType.VOTE,
    ) : ActionInfo {
        @IgnoredOnParcel
        override val isAffectedByRateLimit: Boolean = true
    }

    @Parcelize
    @Serializable
    @SerialName("2")
    data class CommentActionInfo(
        val postRef: PostRef,
        val parentId: CommentId?,
        /**
         * The comment to post
         */
        val content: String,

        override val accountId: Long,
        override val accountInstance: String?,
        override val retries: Int = 0,
        override val action: ActionType = ActionType.COMMENT,
    ) : ActionInfo {
        @IgnoredOnParcel
        override val isAffectedByRateLimit: Boolean = true
    }

    @Parcelize
    @Serializable
    @SerialName("3")
    data class DeleteCommentActionInfo(
        val postRef: PostRef,
        val commentId: CommentId,

        override val accountId: Long,
        override val accountInstance: String?,
        override val retries: Int = 0,
        override val action: ActionType = ActionType.DELETE_COMMENT,
    ) : ActionInfo {
        @IgnoredOnParcel
        override val isAffectedByRateLimit: Boolean = true
    }

    @Parcelize
    @Serializable
    @SerialName("4")
    data class EditCommentActionInfo(
        val postRef: PostRef,
        val commentId: CommentId,
        /**
         * The comment to post
         */
        val content: String,

        override val accountId: Long,
        override val accountInstance: String?,
        override val retries: Int = 0,
        override val action: ActionType = ActionType.COMMENT,
    ) : ActionInfo {
        @IgnoredOnParcel
        override val isAffectedByRateLimit: Boolean = true
    }

    @Parcelize
    @Serializable
    @SerialName("5")
    data class MarkPostAsReadActionInfo(
        val postRef: PostRef,
        val read: Boolean,

        override val accountId: Long,
        override val accountInstance: String?,
        override val retries: Int = 0,
        override val action: ActionType = ActionType.COMMENT,
    ) : ActionInfo {
        @IgnoredOnParcel
        override val isAffectedByRateLimit: Boolean = true
    }

    fun incRetries() = when (this) {
        is CommentActionInfo -> this.copy(retries = this.retries + 1)
        is DeleteCommentActionInfo -> this.copy(retries = this.retries + 1)
        is EditCommentActionInfo -> this.copy(retries = this.retries + 1)
        is VoteActionInfo -> this.copy(retries = this.retries + 1)
        is MarkPostAsReadActionInfo -> this.copy(retries = this.retries + 1)
    }
}

interface LemmyActionResult<T : ActionInfo, R> {

    val result: R

    class VoteLemmyActionResult(
        override val result: Either<PostView, CommentView>,
    ) : LemmyActionResult<ActionInfo.VoteActionInfo, Either<PostView, CommentView>>

    class CommentLemmyActionResult : LemmyActionResult<ActionInfo.CommentActionInfo, Unit> {
        override val result = Unit
    }

    class DeleteCommentLemmyActionResult : LemmyActionResult<ActionInfo.DeleteCommentActionInfo, Unit> {
        override val result = Unit
    }

    class EditLemmyActionResult : LemmyActionResult<ActionInfo.EditCommentActionInfo, Unit> {
        override val result = Unit
    }
    class MarkPostAsReadActionResult : LemmyActionResult<ActionInfo.MarkPostAsReadActionInfo, Unit> {
        override val result = Unit
    }
}

/**
 * Used to deserialize action from db.
 */
enum class ActionType(val code: Int) {
    UNKNOWN(-1),
    VOTE(1),
    COMMENT(2),
    DELETE_COMMENT(3),
}
