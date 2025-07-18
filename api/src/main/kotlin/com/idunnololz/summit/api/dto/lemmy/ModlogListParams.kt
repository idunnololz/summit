package com.idunnololz.summit.api.dto.lemmy

data class ModlogListParams(
  val community_id: CommunityId? = null,
  val mod_person_id: PersonId? = null,
  val other_person_id: PersonId? = null,
  val page: Int? = null,
  val limit: Int? = null,
  val hide_modlog_names: Boolean,
)
