package com.idunnololz.summit.api.dto.lemmy

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommentAggregates(
  val id: Int,
  val comment_id: CommentId,
  val score: Int,
  val upvotes: Int,
  val downvotes: Int,
  val published: String,
  val child_count: Int,
) : Parcelable
