package com.idunnololz.summit.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModRemoveCommentView(
  val mod_remove_comment: ModRemoveComment,
  val moderator: Person? = null,
  val comment: Comment,
  val commenter: Person,
  val post: Post,
  val community: Community,
)
