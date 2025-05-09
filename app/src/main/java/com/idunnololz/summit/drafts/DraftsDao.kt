package com.idunnololz.summit.drafts

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DraftsDao {

  @Query("SELECT * FROM drafts")
  suspend fun getAllDrafts(): List<DraftEntry>

  @Query("SELECT * FROM drafts WHERE uts <= :updateTs ORDER BY uts DESC LIMIT :limit")
  suspend fun getAllDrafts(limit: Int, updateTs: Long): List<DraftEntry>

  @Query(
    "SELECT * FROM drafts WHERE draft_type = :type AND uts <= :updateTs ORDER BY uts DESC LIMIT :limit",
  )
  suspend fun getDraftsByType(type: Int, limit: Int, updateTs: Long): List<DraftEntry>

  @Query(
    "SELECT * FROM drafts WHERE draft_type = :type AND account_id = :accountId AND account_instance = :accountInstance",
  )
  suspend fun getAllDraftsByType(
    type: Int,
    accountId: Long,
    accountInstance: String,
  ): List<DraftEntry>

  @Query("SELECT * FROM drafts WHERE id = :id")
  suspend fun getDraft(id: Long): List<DraftEntry>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entry: DraftEntry): Long

  @Query(
    "UPDATE drafts SET uts = :updateTs, draft_type = :draftType, data = :data WHERE id = :id",
  )
  suspend fun update(id: Long, updateTs: Long, draftType: Int, data: DraftData)

  @Delete
  suspend fun delete(action: DraftEntry)

  @Query("DELETE FROM drafts WHERE id = :entryId")
  fun deleteWithId(entryId: Long)

  @Query("DELETE FROM drafts WHERE draft_type = :type")
  suspend fun deleteAll(type: Int)

  @Query("DELETE FROM drafts")
  suspend fun deleteAll()

  @Query("SELECT COUNT(*) FROM drafts")
  suspend fun count(): Int
}
