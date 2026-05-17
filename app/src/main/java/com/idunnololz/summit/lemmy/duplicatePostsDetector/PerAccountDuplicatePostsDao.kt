package com.idunnololz.summit.lemmy.duplicatePostsDetector

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PerAccountDuplicatePostsDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE, entity = PerAccountDuplicatePostsEntry::class)
  fun insert(entry: PerAccountDuplicatePostsEntry)

  @Query("SELECT * FROM duplicate_posts_entry WHERE userId = :userId")
  suspend fun getEntry(userId: Long): List<PerAccountDuplicatePostsEntry>
}
