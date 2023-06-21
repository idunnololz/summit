package com.idunnololz.summit.lemmy.actions

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
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import io.reactivex.Completable
import io.reactivex.Single

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
    val info: ActionInfo?
)

@ProvidedTypeConverter
class LemmyActionConverters(private val moshi: Moshi) {
    @TypeConverter
    fun actionInfoToString(value: ActionInfo): String {
        return moshi.adapter(ActionInfo::class.java).toJson(value)
    }

    @TypeConverter
    fun stringToActionInfo(value: String): ActionInfo? =
        try {
            moshi.adapter(ActionInfo::class.java).fromJson(value)
        } catch (e: Exception) {
            null
        }
}

sealed interface ActionInfo {

    companion object {
        fun adapter(): PolymorphicJsonAdapterFactory<ActionInfo> =
            PolymorphicJsonAdapterFactory.of(ActionInfo::class.java, "t")
                .withSubtype(ActionInfo.VoteActionInfo::class.java, "1")
                .withSubtype(ActionInfo.CommentActionInfo::class.java, "2")
                .withSubtype(ActionInfo.DeleteCommentActionInfo::class.java, "3")
                .withSubtype(ActionInfo.EditActionInfo::class.java, "4")
                .withDefaultValue(null)

    }

    val accountId: Int?
    val action: ActionType
    val isAffectedByRateLimit: Boolean
    val retries: Int

    @JsonClass(generateAdapter = true)
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
    data class CommentActionInfo(
        val instance: String,
        /**
         * Id of what to comment on
         */
        val parentId: String,
        /**
         * The comment to post
         */
        val text: String,
        override val accountId: Int,
        override val retries: Int = 0,
        override val action: ActionType = ActionType.COMMENT,
    ) : ActionInfo {
        override val isAffectedByRateLimit: Boolean = true
    }

    @JsonClass(generateAdapter = true)
    data class DeleteCommentActionInfo(
        val instance: String,
        /**
         * Full id of the comment to delete
         */
        val id: String,
        override val accountId: Int,
        override val retries: Int = 0,
        override val action: ActionType = ActionType.DELETE_COMMENT,
    ) : ActionInfo {
        override val isAffectedByRateLimit: Boolean = true
    }

    @JsonClass(generateAdapter = true)
    data class EditActionInfo(
        val instance: String,
        /**
         * Id of what to edit
         */
        val thingId: String,
        /**
         * The new text
         */
        val text: String,
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
        }

}

/**
 * Used to deserialize action from db.
 */
enum class ActionType constructor(val code: Int) {
    UNKNOWN(-1),
    VOTE(1),
    COMMENT(2),
    DELETE_COMMENT(3);

    companion object {
        fun fromCode(code: Int): ActionType = values().first { it.ordinal == code }
    }
}