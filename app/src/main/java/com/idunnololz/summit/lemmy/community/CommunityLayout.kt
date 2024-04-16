package com.idunnololz.summit.lemmy.community

enum class CommunityLayout {
    Compact,
    List,
    LargeList,
    Card,
    Card2,
    Card3,
    Full,
    ListWithCards,
}

fun CommunityLayout.usesDividers(): Boolean = when (this) {
    CommunityLayout.Compact -> true
    CommunityLayout.List -> true
    CommunityLayout.LargeList -> true
    CommunityLayout.Card -> false
    CommunityLayout.Card2 -> false
    CommunityLayout.Card3 -> false
    CommunityLayout.Full -> true
    CommunityLayout.ListWithCards -> false
}
