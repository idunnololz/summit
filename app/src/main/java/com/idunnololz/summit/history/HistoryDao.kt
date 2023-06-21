package com.idunnololz.summit.history

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Single

/**
 * Data Access Object
 */
@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY ts")
    suspend fun getAllHistoryEntries(): List<HistoryEntry>

    @Query("SELECT id, url, shortDesc, ts, type, reason FROM history ORDER BY ts DESC")
    suspend fun getAllLiteHistoryEntries(): List<LiteHistoryEntry>

    @Query("SELECT * FROM history WHERE id = :entryId")
    suspend fun getHistoryEntry(entryId: Long): HistoryEntry?

    @Query("SELECT * FROM history ORDER BY ts DESC LIMIT 1")
    suspend fun getLastHistoryEntry(): HistoryEntry?

    @Query("SELECT * FROM history WHERE type = :type ORDER BY ts DESC LIMIT 1")
    suspend fun getLastHistoryEntryWithType(type: Int): HistoryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(historyEntry: HistoryEntry): Long

    @Delete
    suspend fun delete(historyEntry: HistoryEntry)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAllHistoryEntries()
}