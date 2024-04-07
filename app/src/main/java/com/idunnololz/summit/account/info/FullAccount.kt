package com.idunnololz.summit.account.info

import com.idunnololz.summit.account.Account
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PersonRef

data class FullAccount(
    val account: Account,
    val accountInfo: AccountInfo,
) {
    val accountId = account.id
}

fun FullAccount.isPersonBlocked(personRef: PersonRef): Boolean =
    accountInfo.miscAccountInfo?.blockedPersons?.any { it.personRef == personRef } == true

fun FullAccount.isCommunityBlocked(communityRef: CommunityRef.CommunityRefByName): Boolean =
    accountInfo.miscAccountInfo?.blockedCommunities?.any { it.communityRef == communityRef } == true