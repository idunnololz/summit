package com.idunnololz.summit.hidePosts

import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.idunnololz.summit.history.HistoryEntry

private const val HIDDEN_POSTS_LIMIT = 1000

@Dao
interface HiddenPostsDao {

    @Query("SELECT * FROM hidden_posts WHERE instance = :instance")
    suspend fun getHiddenPosts(instance: String): List<HiddenPostEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenPost(hiddenPostEntry: HiddenPostEntry): Long

    @Query("DELETE FROM hidden_posts WHERE id IN (SELECT id FROM hidden_posts ORDER BY ts DESC " +
            "LIMIT 10 OFFSET $HIDDEN_POSTS_LIMIT)")
    suspend fun pruneDb()

    @Query("SELECT count(*) FROM hidden_posts")
    suspend fun count(): Long

    @Delete
    suspend fun delete(historyEntry: HistoryEntry)

    @Query("DELETE FROM hidden_posts")
    suspend fun clear()

    @Transaction
    open suspend fun insertHiddenPostRespectingTableLimit(newEntry: HiddenPostEntry) {
        pruneDb()
        insertHiddenPost(newEntry)
    }
}