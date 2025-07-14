package com.idunnololz.summit.api.dto.lemmy

data class Site(
  val id: SiteId,
  val name: String,
  val sidebar: String? = null,
  val published: String? = null,
  val updated: String? = null,
  val icon: String? = null,
  val banner: String? = null,
  val description: String? = null,
  val actor_id: String,
  val last_refreshed_at: String? = null,
  val inbox_url: String? = null,
  val private_key: String? = null,
  val public_key: String? = null,
  val instance_id: InstanceId? = null,
)
