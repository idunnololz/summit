package com.idunnololz.summit.api.dto.lemmy

data class Tagline(
  val id: Int,
  val local_site_id: LocalSiteId,
  val content: String,
  val published: String,
  val updated: String? = null,
)
