package com.idunnololz.summit.user

import com.idunnololz.summit.lemmy.CommunityViewState
import kotlinx.serialization.Serializable

@Serializable
data class TabCommunityState(
    val tabId: Long?,
    val viewState: CommunityViewState?,
)
