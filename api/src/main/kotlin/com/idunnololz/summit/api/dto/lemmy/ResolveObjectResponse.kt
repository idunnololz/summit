package com.idunnololz.summit.api.dto.lemmy

data class ResolveObjectResponse(
  val comment: CommentView? = null,
  val post: PostView? = null,
  val community: CommunityView? = null,
  val person: PersonView? = null,
)
