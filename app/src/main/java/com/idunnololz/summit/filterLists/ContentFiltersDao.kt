package com.idunnololz.summit.filterLists

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContentFiltersDao {

    @Query("SELECT * FROM content_filters")
    suspend fun getAllFilters(): List<FilterEntry>

    @Query("SELECT * FROM content_filters WHERE contentType = :contentTypeId")
    suspend fun getFiltersForContentType(contentTypeId: ContentTypeId): List<FilterEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(entry: FilterEntry): Long

    @Query("SELECT count(*) FROM content_filters")
    suspend fun count(): Long

    @Delete
    suspend fun delete(entry: FilterEntry)

    @Query("DELETE FROM content_filters")
    suspend fun clear()
}
