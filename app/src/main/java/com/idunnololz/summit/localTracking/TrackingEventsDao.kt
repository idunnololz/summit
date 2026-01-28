package com.idunnololz.summit.localTracking

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery

@Dao
interface TrackingEventsDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrackingEventEntry(entry: TrackingEventEntry)

  @Query("SELECT * FROM tracking_events")
  suspend fun getAll(): List<TrackingEventEntry>

  @Query("SELECT * FROM tracking_events WHERE event_action = :action")
  suspend fun getEventsWithAction(action: TrackedAction): List<TrackingEventEntry>

  @Query("SELECT COUNT(id) FROM tracking_events")
  fun getCount(): Int

  @RawQuery
  fun getTableSize(query: RoomRawQuery): Long

  @Query("DELETE FROM tracking_events")
  suspend fun deleteAll()
}

fun TrackingEventsDao.getTableSize() =
  getTableSize(
    RoomRawQuery(
      sql = "SELECT SUM(\"pgsize\") FROM \"dbstat\" WHERE name='tracking_events';",
      onBindStatement = { },
    ),
  )