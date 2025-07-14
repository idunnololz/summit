package com.idunnololz.summit.api.dto.lemmy

data class CommunityResponse(
  val community_view: CommunityView,
  val discussion_languages: List<LanguageId>,
)
