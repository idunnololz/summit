package com.idunnololz.summit.lemmy.utils

import com.idunnololz.summit.account.info.FullAccount
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.toSortOrder
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.perCommunity.PerCommunityPreferences


fun getSortOrderForCommunity(
    communityRef: CommunityRef?,
    preferences: Preferences,
    perCommunityPreferences: PerCommunityPreferences,
    fullAccount: FullAccount?,
): CommunitySortOrder? {
    if (communityRef != null) {
        val config = perCommunityPreferences.getCommunityConfig(communityRef)
        val sortOrder = config?.sortOrder

        if (sortOrder != null) {
            return sortOrder
        }
    }

    if (preferences.defaultCommunitySortOrder != null) {
        return preferences.defaultCommunitySortOrder
    }

    if (fullAccount != null) {
        return fullAccount
            .accountInfo
            .miscAccountInfo
            ?.defaultCommunitySortType
            ?.toSortOrder()
    }

    return null
}