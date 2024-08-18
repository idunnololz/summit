package com.idunnololz.summit.lemmy.inbox.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConversationEntriesDao {

    @Query("SELECT * FROM conversation_entries WHERE account_full_name = :accountFullName")
    suspend fun getAllEntriesForAccount(accountFullName: String): List<ConversationEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ConversationEntry): Long

    @Delete
    suspend fun delete(entry: ConversationEntry)

    @Query("DELETE FROM conversation_entries")
    suspend fun deleteAll()

    @Query("DELETE FROM conversation_entries WHERE account_full_name = :accountFullName")
    suspend fun deleteConversationsForAccount(accountFullName: String)
}
