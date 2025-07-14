package com.idunnololz.summit.api.dto.lemmy

import kotlinx.serialization.Serializable

@Serializable
data class AdminPurgeCommentView(
  val admin_purge_comment: AdminPurgeComment,
  val admin: Person? = null,
  val post: Post,
)
