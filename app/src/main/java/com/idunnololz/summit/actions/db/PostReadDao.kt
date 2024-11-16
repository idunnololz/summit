package com.idunnololz.summit.actions.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idunnololz.summit.actions.PostReadManager.Companion.MAX_READ_POST_LIMIT

@Dao
interface PostReadDao {
    @Query("SELECT * FROM read_posts LIMIT $MAX_READ_POST_LIMIT")
    suspend fun getAll(): List<ReadPostEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = ReadPostEntry::class)
    suspend fun insert(entry: ReadPostEntry)

    @Query("DELETE FROM read_posts WHERE post_key = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM read_posts")
    suspend fun deleteAll()
}
