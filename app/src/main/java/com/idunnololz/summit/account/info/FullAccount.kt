package com.idunnololz.summit.account.info

import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toPersonRef

class FullAccount(
    val account: Account,
    val accountInfo: AccountInfo,
) {
    val accountId = account.id
}

fun FullAccount.isPersonBlocked(personRef: PersonRef): Boolean =
    accountInfo.miscAccountInfo?.blockedPersons?.any { it.personRef == personRef } == true

fun FullAccount.isCommunityBlocked(communityRef: CommunityRef.CommunityRefByName): Boolean =
    accountInfo.miscAccountInfo?.blockedCommunities?.any { it.communityRef == communityRef } == true

fun GetSiteResponse.toFullAccount(account: Account): FullAccount {
    val localUserView = this.my_user?.local_user_view
    val accountInfo = AccountInfo(
        accountId = account.id,
        subscriptions = this.my_user
            ?.follows
            ?.map { it.community.toAccountSubscription() }
            ?: listOf(),
        miscAccountInfo = MiscAccountInfo(
            avatar = localUserView?.person?.avatar,
            defaultCommunitySortType = localUserView?.local_user?.default_sort_type,
            showReadPosts = localUserView?.local_user?.show_read_posts,
            modCommunityIds = this.my_user?.moderates?.map { it.community.id },
            isAdmin = this.admins.firstOrNull { it.person.id == account.id } != null,
            blockedPersons = this.my_user?.person_blocks?.map {
                BlockedPerson(
                    personId = it.target.id,
                    personRef = it.target.toPersonRef(),
                )
            },
            blockedCommunities = this.my_user?.community_blocks?.map {
                BlockedCommunity(
                    it.community.id,
                    it.community.toCommunityRef(),
                )
            },
            blockedInstances = this.my_user?.instance_blocks?.map {
                BlockedInstance(
                    it.instance.id,
                    it.instance.domain,
                )
            },
        ),
    )
    return FullAccount(
        account,
        accountInfo,
    )
}