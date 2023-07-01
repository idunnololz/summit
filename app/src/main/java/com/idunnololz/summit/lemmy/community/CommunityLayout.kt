package com.idunnololz.summit.lemmy.community

import com.idunnololz.summit.lemmy.post_view.PostUiConfig

enum class CommunityLayout {
    Compact,
    List,
    Card,
    Full
}


fun CommunityLayout.usesDividers(): Boolean =
    when (this) {
        CommunityLayout.Compact -> true
        CommunityLayout.List -> true
        CommunityLayout.Card -> false
        CommunityLayout.Full -> true
    }