package com.idunnololz.summit.api.dto.lemmy

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Comment(
  val id: CommentId,
  val creator_id: PersonId,
  val post_id: PostId,
  val content: String,
  val removed: Boolean,
  val published: String,
  val updated: String? = null,
  val deleted: Boolean,
  val ap_id: String,
  val local: Boolean,
  val path: String,
  val distinguished: Boolean,
  val language_id: LanguageId,
) : Parcelable
