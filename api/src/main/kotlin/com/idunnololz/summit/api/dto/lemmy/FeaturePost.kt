package com.idunnololz.summit.api.dto.lemmy

data class FeaturePost(
  val post_id: PostId,
  val featured: Boolean,
  val feature_type: PostFeatureType /* "Local" | "Community" */,
  val auth: String,
)
