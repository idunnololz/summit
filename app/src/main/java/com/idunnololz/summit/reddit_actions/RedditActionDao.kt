package com.idunnololz.summit.reddit_actions

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Single

/**
 * Data Access Object
 */
@Dao
interface RedditActionDao {

    @Query("SELECT * FROM reddit_actions")
    fun getAllPendingActions(): Single<List<RedditAction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAction(action: RedditAction): Single<Long>

    @Delete
    fun delete(action: RedditAction): Completable

    @Query("DELETE FROM reddit_actions")
    fun deleteAllActions(): Completable
}