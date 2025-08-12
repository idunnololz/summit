package com.idunnololz.summit.templates.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TemplatesDao {

  @Query("SELECT * FROM templates")
  suspend fun getAllTemplates(): List<TemplateEntry>

  @Query(
    "SELECT * FROM templates WHERE template_type = :type",
  )
  suspend fun getTemplatesByType(type: Int): List<TemplateEntry>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entry: TemplateEntry): Long

  @Query(
    "UPDATE templates SET uts = :updateTs, template_type = :templateType, data = :data WHERE id = :id",
  )
  suspend fun update(id: Long, updateTs: Long, templateType: Int, data: TemplateData)

  @Query("DELETE FROM templates WHERE id = :entryId")
  fun deleteWithId(entryId: Long)
}
