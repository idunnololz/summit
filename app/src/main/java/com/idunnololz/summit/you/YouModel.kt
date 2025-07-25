package com.idunnololz.summit.you

import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.info.AccountInfo
import com.idunnololz.summit.api.dto.lemmy.GetPersonDetailsResponse

data class YouModel(
  val name: String?,
  val account: Account?,
  val accountInfo: AccountInfo?,
  val personResult: Result<GetPersonDetailsResponse>?,
  val supportsUploads: Boolean,
  val isLoading: Boolean,
)
