package com.idunnololz.summit.you

import com.idunnololz.summit.account.info.FullAccount
import com.idunnololz.summit.api.dto.GetPersonDetailsResponse

data class YouModel(
    val name: String?,
    val fullAccount: FullAccount?,
    val personResult: Result<GetPersonDetailsResponse>,
)
