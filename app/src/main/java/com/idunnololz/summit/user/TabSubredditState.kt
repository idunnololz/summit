package com.idunnololz.summit.user

import com.idunnololz.summit.lemmy.CommunityViewState
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TabCommunityState(
    val tabId: Long?,
    val viewState: CommunityViewState?,
)
