package com.idunnololz.summit.localTracking

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.idunnololz.summit.lemmy.CommunityRef
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(tableName = "tracking_events")
class TrackingEventEntry(
  @PrimaryKey(autoGenerate = true)
  val id: Long,
  val userId: Long,
  val ts: Long,

  @ColumnInfo(name = "event_action", defaultValue = "VIEW")
  val action: TrackedAction = TrackedAction.VIEW,

  /**
   * What this filter is for. Eg. post list, comments, etc.
   */
  @ColumnInfo(name = "event", typeAffinity = ColumnInfo.BLOB)
  val trackingEventCbor: ByteArray,
)

@Parcelize
@Serializable
data class TrackingEvent(
  // which user is this even associated with?
  val userId: Long?,

  val instanceId: Long?,
  val communityRef: CommunityRef?,
  val postId: Long?,
  val commentId: Long?,
  val targetUserId: Long?,
  val action: TrackedAction,
  val nsfw: Boolean,
): Parcelable

enum class TrackedAction {
  UPVOTE,
  DOWNVOTE,
  CLEAR_VOTE,

  VIEW,
  REPLY,
  DELETE_REPLY,
}