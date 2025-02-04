package com.idunnololz.summit.lemmy.actions

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverters

private const val COMPLETED_ACTIONS_LIMIT = 150

@Dao
interface LemmyCompletedActionsDao {

    @Query("SELECT * FROM lemmy_completed_actions")
    suspend fun getAllCompletedActions(): List<LemmyCompletedAction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: LemmyCompletedAction): Long

    @Delete
    suspend fun delete(action: LemmyCompletedAction)

    @Query("DELETE FROM lemmy_completed_actions")
    suspend fun deleteAllActions()

    @Query("SELECT COUNT(*) FROM lemmy_completed_actions")
    suspend fun count(): Int

    @Query(
        "DELETE FROM lemmy_completed_actions WHERE id IN (SELECT id FROM lemmy_completed_actions ORDER BY ts DESC " +
            "LIMIT 10 OFFSET $COMPLETED_ACTIONS_LIMIT)",
    )
    suspend fun pruneDb()

    @Transaction
    open suspend fun insertActionRespectingTableLimit(newEntry: LemmyCompletedAction) {
        pruneDb()
        insertAction(newEntry)
    }
}

@Entity(tableName = "lemmy_completed_actions")
@TypeConverters(LemmyActionConverters::class)
data class LemmyCompletedAction(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    override val id: Long,
    @ColumnInfo(name = "ts")
    override val ts: Long,
    @ColumnInfo(name = "cts")
    override val creationTs: Long,
    @ColumnInfo(name = "info")
    override val info: ActionInfo?,
): LemmyAction
