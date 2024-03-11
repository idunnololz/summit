package com.idunnololz.summit.lemmy.inbox

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

private const val INBOX_ENTRIES_LIMIT = 10_000

@Dao
interface InboxEntriesDao {

    @Query("SELECT * FROM inbox_entries")
    suspend fun getAllInboxEntries(): List<InboxEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(inboxEntry: InboxEntry): Long

    @Delete
    suspend fun delete(inboxEntry: InboxEntry)

    @Query("DELETE FROM inbox_entries")
    suspend fun deleteAllActions()

    @Query("SELECT COUNT(*) FROM inbox_entries")
    suspend fun count(): Int

    @Query("SELECT * FROM inbox_entries WHERE notification_id = :notificationId")
    suspend fun findInboxEntries(notificationId: Int): List<InboxEntry>

    @Query(
        "DELETE FROM inbox_entries WHERE id IN (SELECT id FROM inbox_entries ORDER BY ts DESC " +
            "LIMIT 10 OFFSET $INBOX_ENTRIES_LIMIT)",
    )
    suspend fun pruneDb()
}