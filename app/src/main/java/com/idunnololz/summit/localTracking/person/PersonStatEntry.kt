package com.idunnololz.summit.localTracking.person

import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

@Entity(tableName = "person_tracker", primaryKeys = ["userId", "targetPersonId"])
class PersonStatEntry(
  val userId: Long,
  val targetPersonId: Long,

  val lastUpdateTs: Long,
  val createdTs: Long,

  val totalScore: Int,

  @ColumnInfo(name = "personIntermediateStats", typeAffinity = ColumnInfo.BLOB)
  val personIntermediateStatsCbor: ByteArray,
)

class SimplePersonStatEntry(
  val userId: Long,
  val targetPersonId: Long,

  val lastUpdateTs: Long,
  val createdTs: Long,

  val totalScore: Int,
)

@Serializable
class PersonIntermediateStats(
  val upvoted: MutableSet<String> = mutableSetOf(),
  val downvoted: MutableSet<String> = mutableSetOf(),
)

@OptIn(ExperimentalSerializationApi::class)
fun PersonStatEntry.getPersonIntermediateStats(): PersonIntermediateStats =
  Cbor.decodeFromByteArray(personIntermediateStatsCbor)
