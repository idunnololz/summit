package com.idunnololz.summit.models

import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.CommunityView
import com.idunnololz.summit.api.dto.lemmy.PersonView
import com.idunnololz.summit.api.dto.lemmy.SearchType

data class SearchResponse(
  val type: SearchType /* "All" | "Comments" | "Posts" | "Communities" | "Users" | "Url" */,
  val comments: List<CommentView>,
  val posts: List<PostView>,
  val communities: List<CommunityView>,
  val users: List<PersonView>,
)
