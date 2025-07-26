package com.idunnololz.summit.lemmy.mod.viewVotes

import com.idunnololz.summit.api.dto.lemmy.VoteView
import com.idunnololz.summit.lemmy.utils.listSource.PageResult

class VotesModel(
  val pages: MutableList<PageResult.SuccessPageResult<VoteView>>,
)
