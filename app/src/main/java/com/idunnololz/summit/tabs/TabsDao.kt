package com.idunnololz.summit.tabs

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Single

/**
 * Data Access Object
 */
@Dao
interface TabsDao {

    @Query("SELECT * FROM tabs ORDER BY sortId")
    fun getAllTabs(): Single<List<TabEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTab(tab: TabEntry): Single<Long>

    @Delete
    fun delete(tab: TabEntry): Completable

    @Query("DELETE FROM tabs")
    fun deleteTabs(): Completable
}