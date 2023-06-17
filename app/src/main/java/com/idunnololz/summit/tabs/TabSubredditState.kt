package com.idunnololz.summit.tabs

import com.idunnololz.summit.lemmy.CommunityViewState
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TabCommunityState(
    val tabId: String,
    val viewState: CommunityViewState
)