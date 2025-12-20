package com.idunnololz.summit.localTracking

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackingEventsDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrackingEventEntry(entry: TrackingEventEntry)

  @Query("SELECT * FROM tracking_events")
  suspend fun getAll(): List<TrackingEventEntry>
}