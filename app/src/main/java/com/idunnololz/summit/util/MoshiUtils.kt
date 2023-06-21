package com.idunnololz.summit.util

import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.RecentCommunityManager
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.squareup.moshi.Moshi

val moshi: Moshi by lazy {
    Moshi.Builder()
        .add(CommunityRef.adapter())
        .add(ActionInfo.adapter())
        .add(VotableRef.adapter())
//        .add(CommunitySortOrder.adapter())
        .build()
}