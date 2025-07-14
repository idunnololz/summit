package com.idunnololz.summit.api.dto.lemmy

import kotlinx.serialization.Serializable

@Serializable
data class ModRemovePostView(
  val mod_remove_post: ModRemovePost,
  val moderator: Person? = null,
  val post: Post,
  val community: Community,
)
