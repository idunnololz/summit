package com.idunnololz.summit.emoji.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

const val MAX_TEXT_EMOJIS = 200

@Dao
interface TextEmojiDao {
    @Query("SELECT * FROM text_emojis LIMIT $MAX_TEXT_EMOJIS")
    suspend fun getAll(): List<TextEmojiEntry>

    @Query("SELECT * FROM text_emojis WHERE id = :id")
    suspend fun getEntry(id: Long): List<TextEmojiEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = TextEmojiEntry::class)
    suspend fun insert(entry: TextEmojiEntry)

    @Query("DELETE FROM text_emojis WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM text_emojis")
    suspend fun deleteAll()
}