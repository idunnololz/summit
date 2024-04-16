package com.idunnololz.summit.lemmy.communityInfo

import arrow.core.Either
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.GetCommunityResponse
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.dto.SiteView
import com.idunnololz.summit.api.dto.SubscribedType
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.util.dateStringToTs

data class CommunityInfoData(
    val backingObject: Either<CommunityView, SiteView>,
    val name: String,
    val fullName: String,
    val iconUrl: String?,
    val bannerUrl: String?,
    val instance: String,
    val publishTs: Int,
    val subscribedStatus: SubscribedType,
    val canSubscribe: Boolean,
    val content: String?,
    val isHidden: Boolean,
    val isRemoved: Boolean,
    val isDeleted: Boolean,

    val postCount: Int,
    val commentCount: Int,
    val userCount: Int,
    val usersPerDay: Int,
    val usersPerWeek: Int,
    val usersPerMonth: Int,
    val usersPerSixMonth: Int,

    val mods: List<Person>,
    val admins: List<PersonView>,
)

fun GetCommunityResponse.toPageData(): CommunityInfoData {
    val communityView = this.community_view
    val name = communityView.community.name
    val instance = communityView.community.instance

    return CommunityInfoData(
        backingObject = Either.Left(communityView),
        name = if (communityView.community.title.isBlank()) {
            name
        } else {
            communityView.community.title
        },
        fullName = "!$name@$instance",
        iconUrl = communityView.community.icon,
        bannerUrl = communityView.community.banner,
        instance = communityView.community.instance,
        publishTs = dateStringToTs(communityView.community.published).toInt(),
        subscribedStatus = communityView.subscribed,
        canSubscribe = true,
        content = communityView.community.description,
        isHidden = communityView.community.hidden,
        isRemoved = communityView.community.removed,
        isDeleted = communityView.community.deleted,

        postCount = communityView.counts.posts,
        commentCount = communityView.counts.comments,
        userCount = communityView.counts.subscribers,
        usersPerDay = communityView.counts.users_active_day,
        usersPerWeek = communityView.counts.users_active_week,
        usersPerMonth = communityView.counts.users_active_month,
        usersPerSixMonth = communityView.counts.users_active_half_year,

        mods = this.moderators.map { it.moderator },
        admins = listOf(),
    )
}

fun GetSiteResponse.toPageData(): CommunityInfoData {
    val siteView = this.site_view
    return CommunityInfoData(
        backingObject = Either.Right(siteView),
        name = siteView.site.name,
        fullName = "!${siteView.site.instance}",
        iconUrl = siteView.site.icon,
        bannerUrl = siteView.site.banner,
        instance = siteView.site.instance,
        publishTs = dateStringToTs(siteView.site.published).toInt(),
        subscribedStatus = SubscribedType.NotSubscribed,
        canSubscribe = false,
        content = siteView.site.sidebar,
        isHidden = false,
        isRemoved = false,
        isDeleted = false,

        postCount = siteView.counts.posts,
        commentCount = siteView.counts.comments,
        userCount = siteView.counts.users,
        usersPerDay = siteView.counts.users_active_day,
        usersPerWeek = siteView.counts.users_active_week,
        usersPerMonth = siteView.counts.users_active_month,
        usersPerSixMonth = siteView.counts.users_active_half_year,

        mods = listOf(),
        admins = this.admins,
    )
}
