package com.idunnololz.summit.tabs

import com.idunnololz.summit.reddit.SubredditViewState

data class TabSubredditState(
    val tabId: String,
    val viewState: SubredditViewState
)