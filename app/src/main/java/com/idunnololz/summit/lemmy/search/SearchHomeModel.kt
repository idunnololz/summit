package com.idunnololz.summit.lemmy.search

import com.idunnololz.summit.account.info.AccountSubscription
import com.idunnololz.summit.api.summit.CommunitySuggestionsDto
import com.idunnololz.summit.lemmy.CommunityRef

data class SearchHomeModel(
    val suggestions: List<String>,
    val myCommunities: List<MyCommunity>,
    val communitySuggestionsDto: CommunitySuggestionsDto?,
    val isCommunitySuggestionsLoading: Boolean,
)

data class MyCommunity(
    val communityRef: CommunityRef,
    val sub: AccountSubscription,
)
