package com.idunnololz.summit.lemmy.actions

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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@Dao
interface LemmyActionsDao {

    @Query("SELECT * FROM lemmy_actions")
    suspend fun getAllPendingActions(): List<LemmyAction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: LemmyAction): Long

    @Delete
    suspend fun delete(action: LemmyAction)

    @Query("DELETE FROM lemmy_actions")
    suspend fun deleteAllActions()

    @Query("SELECT COUNT(*) FROM lemmy_actions")
    suspend fun count(): Int
}

@Entity(tableName = "lemmy_actions")
@TypeConverters(LemmyActionConverters::class)
data class LemmyAction(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "ts")
    val ts: Long,
    @ColumnInfo(name = "cts")
    val creationTs: Long,
    @ColumnInfo(name = "info")
    val info: ActionInfo?,
)

@ProvidedTypeConverter
class LemmyActionConverters(private val moshi: Moshi) {

    companion object {
        private val TAG = "LemmyActionConverters"
    }

    @TypeConverter
    fun actionInfoToString(value: ActionInfo): String {
        return moshi.adapter(ActionInfo::class.java).toJson(value)
    }

    @TypeConverter
    fun stringToActionInfo(value: String): ActionInfo? =
        try {
            moshi.adapter(ActionInfo::class.java).fromJson(value)
        } catch (e: Exception) {
            Log.e(TAG, "", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            null
        }

    @TypeConverter
    fun lemmyActionFailureReasonToString(value: LemmyActionFailureReason): String {
        return moshi.adapter(LemmyActionFailureReason::class.java).toJson(value)
    }

    @TypeConverter
    fun stringToLemmyActionFailureReason(value: String): LemmyActionFailureReason? =
        try {
            moshi.adapter(LemmyActionFailureReason::class.java).fromJson(value)
        } catch (e: Exception) {
            Log.e(TAG, "", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
}

@JsonClass(generateAdapter = true, generator = "sealed:t")
sealed interface ActionInfo {

    val accountId: Int?
    val action: ActionType
    val isAffectedByRateLimit: Boolean
    val retries: Int

    @JsonClass(generateAdapter = true)
    @TypeLabel("1")
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
        override val accountId: Int,
        override val retries: Int = 0,
        override val action: ActionType = ActionType.VOTE,
    ) : ActionInfo {
        override val isAffectedByRateLimit: Boolean = true
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("2")
    data class CommentActionInfo(
        val postRef: PostRef,
        val parentId: CommentId?,
        /**
         * The comment to post
         */
        val content: String,

        override val accountId: Int,
        override val retries: Int = 0,
        override val action: ActionType = ActionType.COMMENT,
    ) : ActionInfo {
        override val isAffectedByRateLimit: Boolean = true
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("3")
    data class DeleteCommentActionInfo(
        val postRef: PostRef,
        val commentId: CommentId,

        override val accountId: Int,
        override val retries: Int = 0,
        override val action: ActionType = ActionType.DELETE_COMMENT,
    ) : ActionInfo {
        override val isAffectedByRateLimit: Boolean = true
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("4")
    data class EditActionInfo(
        val postRef: PostRef,
        val commentId: CommentId,
        /**
         * The comment to post
         */
        val content: String,

        override val accountId: Int,
        override val retries: Int = 0,
        override val action: ActionType = ActionType.COMMENT,
    ) : ActionInfo {
        override val isAffectedByRateLimit: Boolean = true
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("5")
    data class MarkPostAsReadActionInfo(
        val postRef: PostRef,
        val read: Boolean,

        override val accountId: Int,
        override val retries: Int = 0,
        override val action: ActionType = ActionType.COMMENT,
    ) : ActionInfo {
        override val isAffectedByRateLimit: Boolean = true
    }

    fun incRetries() =
        when (this) {
            is CommentActionInfo -> this.copy(retries = this.retries + 1)
            is DeleteCommentActionInfo -> this.copy(retries = this.retries + 1)
            is EditActionInfo -> this.copy(retries = this.retries + 1)
            is VoteActionInfo -> this.copy(retries = this.retries + 1)
            is MarkPostAsReadActionInfo -> this.copy(retries = this.retries + 1)
        }
}

interface LemmyActionResult<T : ActionInfo, R> {

    val result: R

    class VoteLemmyActionResult(
        override val result: Either<PostView, CommentView>,
    ) : LemmyActionResult<ActionInfo.VoteActionInfo, Either<PostView, CommentView>>

    class CommentLemmyActionResult() : LemmyActionResult<ActionInfo.CommentActionInfo, Unit> {
        override val result = Unit
    }

    class DeleteCommentLemmyActionResult() : LemmyActionResult<ActionInfo.DeleteCommentActionInfo, Unit> {
        override val result = Unit
    }

    class EditLemmyActionResult() : LemmyActionResult<ActionInfo.EditActionInfo, Unit> {
        override val result = Unit
    }
    class MarkPostAsReadActionResult() : LemmyActionResult<ActionInfo.MarkPostAsReadActionInfo, Unit> {
        override val result = Unit
    }
}

/**
 * Used to deserialize action from db.
 */
enum class ActionType constructor(val code: Int) {
    UNKNOWN(-1),
    VOTE(1),
    COMMENT(2),
    DELETE_COMMENT(3),
    ;

    companion object {
        fun fromCode(code: Int): ActionType = values().first { it.ordinal == code }
    }
}
