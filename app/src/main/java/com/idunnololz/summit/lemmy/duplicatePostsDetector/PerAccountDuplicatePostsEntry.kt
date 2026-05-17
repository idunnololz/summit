package com.idunnololz.summit.lemmy.duplicatePostsDetector

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "duplicate_posts_entry")
class PerAccountDuplicatePostsEntry(
  @PrimaryKey
  val userId: Long,
  @ColumnInfo(name = "uts")
  val updatedTs: Long,
  @ColumnInfo(name = "event", typeAffinity = ColumnInfo.BLOB)
  val trackingEventCbor: ByteArray,
)

@Serializable
class PerAccountDuplicatePostsData(
  val postKeys: List<Pair<String, Long>>,
)
