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
    fun getAllHistoryEntries(): Single<List<HistoryEntry>>

    @Query("SELECT id, url, shortDesc, ts, type, reason FROM history ORDER BY ts DESC")
    fun getAllLiteHistoryEntries(): Single<List<LiteHistoryEntry>>

    @Query("SELECT * FROM history WHERE id = :entryId")
    fun getHistoryEntry(entryId: Long): Single<HistoryEntry>

    @Query("SELECT * FROM history ORDER BY ts DESC LIMIT 1")
    fun getLastHistoryEntry(): Single<HistoryEntry>

    @Query("SELECT * FROM history WHERE type = :type ORDER BY ts DESC LIMIT 1")
    fun getLastHistoryEntryWithType(type: Int): Single<HistoryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistoryEntry(historyEntry: HistoryEntry): Single<Long>

    @Delete
    fun delete(historyEntry: HistoryEntry): Completable

    @Query("DELETE FROM history WHERE id = :id")
    fun deleteById(id: Long): Completable

    @Query("DELETE FROM history")
    fun deleteAllHistoryEntries(): Completable
}