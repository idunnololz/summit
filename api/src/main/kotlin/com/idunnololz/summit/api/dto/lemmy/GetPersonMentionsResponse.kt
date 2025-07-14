package com.idunnololz.summit.api.dto.lemmy

data class GetPersonMentionsResponse(
  val mentions: List<PersonMentionView>,
)
