package com.idunnololz.summit.localTracking.community

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.lemmy.CommunityRef

@Dao
interface CommunityTrackerDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entry: CommunityStatEntry): Long

  @Query("SELECT * FROM community_tracker WHERE communityRef = :communityRef")
  abstract suspend fun getEntryByCommunityRef(communityRef: CommunityRef): List<CommunityStatEntry>

  @Query("SELECT * FROM community_tracker")
  suspend fun getAll(): List<CommunityStatEntry>

  @Query("SELECT * FROM community_tracker ORDER BY hits DESC LIMIT 1000")
  suspend fun getTop1000(): List<CommunityStatEntry>

  @Query("DELETE FROM community_tracker")
  suspend fun deleteAll()
}