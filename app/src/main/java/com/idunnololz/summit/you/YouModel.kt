package com.idunnololz.summit.you

import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.info.AccountInfo
import com.idunnololz.summit.account.info.FullAccount
import com.idunnololz.summit.api.dto.GetPersonDetailsResponse

data class YouModel(
    val name: String?,
    val account: Account?,
    val accountInfo: AccountInfo?,
    val personResult: Result<GetPersonDetailsResponse>?,
    val isLoading: Boolean,
)
