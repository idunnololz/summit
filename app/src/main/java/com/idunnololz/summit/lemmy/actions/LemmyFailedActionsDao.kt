package com.idunnololz.summit.lemmy.actions

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Dao
interface LemmyFailedActionsDao {

    @Query("SELECT * FROM lemmy_failed_actions")
    suspend fun getAllFailedActions(): List<LemmyFailedAction>

    @Query("SELECT * FROM lemmy_failed_actions ORDER BY fts DESC LIMIT 100")
    suspend fun getLast100FailedActions(): List<LemmyFailedAction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFailedAction(action: LemmyFailedAction): Long

    @Delete
    suspend fun delete(action: LemmyFailedAction)

    @Query("DELETE FROM lemmy_failed_actions")
    suspend fun deleteAllFailedActions()

    @Query("SELECT COUNT(*) FROM lemmy_failed_actions")
    suspend fun count(): Int
}

@Entity(tableName = "lemmy_failed_actions")
@TypeConverters(LemmyActionConverters::class)
data class LemmyFailedAction(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    override val id: Long,
    @ColumnInfo(name = "ts")
    override val ts: Long,
    @ColumnInfo(name = "cts")
    override val creationTs: Long,
    @ColumnInfo(name = "fts")
    val failedTs: Long,
    @ColumnInfo(name = "error")
    val error: LemmyActionFailureReason,
    @ColumnInfo(name = "info")
    override val info: ActionInfo?,
    @ColumnInfo(name = "seen")
    val seen: Boolean? = null,
) : LemmyAction

@Serializable
@JsonClassDiscriminator("t")
sealed interface LemmyActionFailureReason : Parcelable {

    @Parcelize
    @Serializable
    @SerialName("1")
    data class RateLimit(
        val recommendedTimeoutMs: Long,
    ) : LemmyActionFailureReason

    @Parcelize
    @Serializable
    @SerialName("2")
    data class TooManyRequests(
        val retries: Int,
    ) : LemmyActionFailureReason

    @Parcelize
    @Serializable
    @SerialName("3")
    data class UnknownError(
        val errorCode: Int,
        val errorMessage: String?,
    ) : LemmyActionFailureReason

    @Parcelize
    @Serializable
    @SerialName("4")
    data class AccountNotFoundError(
        val accountId: Long,
    ) : LemmyActionFailureReason

    @Parcelize
    @Serializable
    @SerialName("5")
    data object NoInternetError : LemmyActionFailureReason

    @Parcelize
    @Serializable
    @SerialName("6")
    data object DeserializationError : LemmyActionFailureReason

    @Parcelize
    @Serializable
    @SerialName("7")
    data object ServerError : LemmyActionFailureReason

    @Parcelize
    @Serializable
    @SerialName("8")
    data object ActionOverwritten : LemmyActionFailureReason

    @Parcelize
    @Serializable
    @SerialName("9")
    data object ConnectionError : LemmyActionFailureReason
}

class LemmyActionFailureException(val reason: LemmyActionFailureReason) : RuntimeException(
    "LemmyAction failed. Cause: ${reason::class.qualifiedName}. Details: $reason",
)
