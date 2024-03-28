package com.idunnololz.summit.account.info

import com.idunnololz.summit.api.dto.CommunityId

fun AccountInfo.isMod(communityId: CommunityId) =
    miscAccountInfo
        ?.modCommunityIds
        ?.contains(communityId) == true
