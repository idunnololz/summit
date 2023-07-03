package com.idunnololz.summit.lemmy.community

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