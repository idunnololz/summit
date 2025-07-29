package com.idunnololz.summit.inbox.conversation

import com.idunnololz.summit.drafts.DraftData

data class DbMessageDraft(
  val entryId: Long,
  val draftData: DraftData.MessageDraftData,
)
