package com.idunnololz.summit.lemmy.mod.viewVotes

import com.idunnololz.summit.api.dto.lemmy.VoteView
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource

class VotesModel(
  val pages: MutableList<LemmyListSource.PageResult<VoteView>>,
)
