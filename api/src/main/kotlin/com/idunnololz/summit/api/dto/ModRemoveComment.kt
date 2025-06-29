package com.idunnololz.summit.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModRemoveComment(
  val id: Int,
  val mod_person_id: PersonId,
  val comment_id: CommentId,
  val reason: String? = null,
  val removed: Boolean,
  val when_: String,
)
