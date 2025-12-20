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
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import arrow.core.Either
import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.PostView
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.util.crashLogger.crashLogger
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator

private const val ACTIONS_LIMIT = 1000

@Dao
interface LemmyActionsDao {

  @Query("SELECT * FROM lemmy_actions WHERE status = NULL or status = 'Pending'")
  suspend fun getAllPendingActions(): List<LemmyAction>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAction(action: LemmyAction): Long

  @Query("SELECT * FROM lemmy_actions WHERE status = 'Errored' ORDER BY fts DESC LIMIT 100")
  suspend fun getLast100FailedActions(): List<LemmyAction>

  @Query("SELECT * FROM lemmy_actions WHERE status = 'Completed'")
  suspend fun getAllCompletedActions(): List<LemmyAction>

  @Query("SELECT * FROM lemmy_actions WHERE status = 'Errored'")
  suspend fun getAllFailedActions(): List<LemmyAction>

  @Query("SELECT * FROM lemmy_actions WHERE id = :actionId")
  suspend fun getActionById(actionId: Long): List<LemmyAction>

  @Delete
  suspend fun delete(action: LemmyAction)

  @Query("DELETE FROM lemmy_actions")
  suspend fun deleteAllActions()

  @Query("DELETE FROM lemmy_actions WHERE status = 'Completed'")
  suspend fun deleteAllCompletedActions()

  @Query("DELETE FROM lemmy_actions WHERE status = 'Errored'")
  suspend fun deleteAllFailedActions()

  @Query("SELECT COUNT(*) FROM lemmy_actions")
  suspend fun count(): Int

  @Query(
    "DELETE FROM lemmy_actions WHERE id IN (SELECT id FROM lemmy_actions ORDER BY ts DESC " +
      "LIMIT 10 OFFSET $ACTIONS_LIMIT)",
  )
  suspend fun pruneDb()

  @Transaction
  open suspend fun insertActionRespectingTableLimit(newEntry: LemmyAction) {
    pruneDb()
    insertAction(newEntry)
  }
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

  @ColumnInfo(name = "fts")
  val failedTs: Long? = null,
  @ColumnInfo(name = "error")
  val error: LemmyActionFailureReason? = null,
  @ColumnInfo(name = "seen")
  val seen: Boolean? = null,

  @ColumnInfo(name = "cots")
  val completedTs: Long? = null,

  @ColumnInfo(name = "status")
  val status: ActionStatus? = null,
)

enum class ActionStatus {
  Pending, Errored, Completed
}

@ProvidedTypeConverter
class LemmyActionConverters(
  private val json: Json,
) {

  companion object {
    private const val TAG = "LemmyActionConverters"
  }

  @TypeConverter
  fun actionInfoToString(value: ActionInfo): String = json.encodeToString(value)

  @TypeConverter
  fun stringToActionInfo(value: String): ActionInfo? = try {
    json.decodeFromString(value)
  } catch (e: Exception) {
    Log.e(TAG, "", e)
    crashLogger?.recordException(e)
    null
  }

  @TypeConverter
  fun lemmyActionFailureReasonToString(value: LemmyActionFailureReason): String =
    json.encodeToString(value)

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
    val languageId: Int? = null,

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
    val languageId: Int? = null,

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

sealed interface LemmyActionResult<T : ActionInfo, R> {

  val result: R

  class VoteLemmyActionResult(
    override val result: Either<PostView, CommentView>,
  ) : LemmyActionResult<ActionInfo.VoteActionInfo, Either<PostView, CommentView>>

  class CommentLemmyActionResult(
    override val result: CommentView,
    val parentId: CommentId?,
  ) : LemmyActionResult<ActionInfo.CommentActionInfo, CommentView>

  class DeleteCommentLemmyActionResult(override val result: CommentView) :
    LemmyActionResult<ActionInfo.DeleteCommentActionInfo, CommentView>

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
enum class ActionType(
  val code: Int,
) {
  UNKNOWN(-1),
  VOTE(1),
  COMMENT(2),
  DELETE_COMMENT(3),
}
