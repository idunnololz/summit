package com.idunnololz.summit.lemmy.mod.viewVotes

import com.idunnololz.summit.api.dto.VoteView
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource

class VotesModel(
  val pages: MutableList<LemmyListSource.PageResult<VoteView>>
)