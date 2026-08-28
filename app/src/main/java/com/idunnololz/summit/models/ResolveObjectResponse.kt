package com.idunnololz.summit.models

import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.CommunityView
import com.idunnololz.summit.api.dto.lemmy.PersonView

data class ResolveObjectResponse(
  val comment: CommentView? = null,
  val post: PostView? = null,
  val community: CommunityView? = null,
  val person: PersonView? = null,
)
