package com.idunnololz.summit.lemmy.userTags

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserTagsDao {

  @Query("SELECT * FROM user_tags")
  suspend fun getAllUserTags(): List<UserTagEntry>

  @Query("SELECT * FROM user_tags ORDER BY used_ts DESC LIMIT 10")
  suspend fun getRecentUserTags(): List<UserTagEntry>

  @Query("SELECT * FROM user_tags WHERE actor_id = :user COLLATE NOCASE")
  suspend fun getUserTagByName(user: String): List<UserTagEntry>

  @Query("SELECT * FROM user_tags WHERE id = :id")
  suspend fun getUserTagById(id: Long): List<UserTagEntry>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUserTag(entry: UserTagEntry): Long

  @Query("SELECT count(*) FROM user_tags")
  suspend fun count(): Long

  @Delete
  suspend fun delete(entry: UserTagEntry)

  @Query("DELETE FROM user_tags")
  suspend fun clear()
}
