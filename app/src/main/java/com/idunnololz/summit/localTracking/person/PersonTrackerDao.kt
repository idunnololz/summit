package com.idunnololz.summit.localTracking.person

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PersonTrackerDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entry: PersonStatEntry)

  @Query("SELECT * FROM person_tracker WHERE userId = :userId AND targetPersonId = :targetPersonId")
  suspend fun getEntry(userId: Long, targetPersonId: Long): List<PersonStatEntry>

  @Query("SELECT userId, targetPersonId, lastUpdateTs, createdTs, totalScore FROM person_tracker")
  suspend fun getAllSimpleEntries(): List<SimplePersonStatEntry>

  @Query("DELETE FROM person_tracker")
  suspend fun deleteAll()
}
