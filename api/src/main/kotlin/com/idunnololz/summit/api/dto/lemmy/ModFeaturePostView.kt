package com.idunnololz.summit.api.dto.lemmy

import kotlinx.serialization.Serializable

@Serializable
data class ModFeaturePostView(
  val mod_feature_post: ModFeaturePost,
  val moderator: Person? = null,
  val post: Post,
  val community: Community,
)
