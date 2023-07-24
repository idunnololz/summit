package com.idunnololz.summit.lemmy.community

enum class CommunityLayout {
    Compact,
    List,
    Card,
    Card2,
    Full
}


fun CommunityLayout.usesDividers(): Boolean =
    when (this) {
        CommunityLayout.Compact -> true
        CommunityLayout.List -> true
        CommunityLayout.Card -> false
        CommunityLayout.Card2 -> false
        CommunityLayout.Full -> true
    }