package com.idunnololz.summit.api.converters

import com.idunnololz.summit.api.dto.lemmy.CommentSortType as CommentSortTypeV3
import com.idunnololz.summit.api.dto.lemmy.ListingType
import com.idunnololz.summit.api.dto.lemmy.ModlogActionType
import com.idunnololz.summit.api.dto.lemmy.SearchType as SearchTypeV3
import com.idunnololz.summit.api.dto.lemmy.SortType
import com.idunnololz.summit.api.dto.lemmy.v4.models.CommentSortType
import com.idunnololz.summit.api.dto.lemmy.v4.models.CommunitySortType
import com.idunnololz.summit.api.dto.lemmy.v4.models.ModlogKindFilter
import com.idunnololz.summit.api.dto.lemmy.v4.models.PostSortType
import com.idunnololz.summit.api.dto.lemmy.v4.models.SearchType
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

fun ListingType.toType() = when (this) {
  ListingType.All ->
    com.idunnololz.summit.api.dto.lemmy.v4.models.ListingType.all
  ListingType.Local ->
    com.idunnololz.summit.api.dto.lemmy.v4.models.ListingType.local
  ListingType.Subscribed ->
    com.idunnololz.summit.api.dto.lemmy.v4.models.ListingType.subscribed
  ListingType.ModeratorView ->
    com.idunnololz.summit.api.dto.lemmy.v4.models.ListingType.moderator_view
}

fun SortType.toPostSortType() = when (this) {
  SortType.Active -> PostSortType.active
  SortType.Hot -> PostSortType.hot
  SortType.New -> PostSortType.new
  SortType.Old -> PostSortType.old
  SortType.TopDay,
  SortType.TopWeek,
  SortType.TopMonth,
  SortType.TopYear,
  SortType.TopHour,
  SortType.TopSixHour,
  SortType.TopTwelveHour,
  SortType.TopThreeMonths,
  SortType.TopSixMonths,
  SortType.TopNineMonths,
  SortType.TopAll,
  -> PostSortType.top
  SortType.MostComments -> PostSortType.most_comments
  SortType.NewComments -> PostSortType.new_comments
  SortType.Controversial -> PostSortType.controversial
  SortType.Scaled -> PostSortType.scaled
}

fun SortType.toCommunitySortType() = when (this) {
  SortType.Active -> CommunitySortType.hot
  SortType.Hot -> CommunitySortType.hot
  SortType.New -> CommunitySortType.new
  SortType.Old -> CommunitySortType.old
  SortType.TopDay -> CommunitySortType.active_daily
  SortType.TopWeek -> CommunitySortType.active_weekly
  SortType.TopMonth -> CommunitySortType.active_monthly
  SortType.TopYear -> CommunitySortType.active_six_months
  SortType.TopHour -> CommunitySortType.active_daily
  SortType.TopSixHour -> CommunitySortType.active_daily
  SortType.TopTwelveHour -> CommunitySortType.active_daily
  SortType.TopThreeMonths -> CommunitySortType.active_six_months
  SortType.TopSixMonths -> CommunitySortType.active_six_months
  SortType.TopNineMonths -> CommunitySortType.active_six_months
  SortType.TopAll -> CommunitySortType.active_six_months
  SortType.MostComments -> CommunitySortType.comments
  SortType.NewComments -> CommunitySortType.comments
  SortType.Controversial -> CommunitySortType.hot
  SortType.Scaled -> CommunitySortType.hot
}

fun SortType.toTimeInSeconds(): Long? = when (this) {
  SortType.TopDay -> 1.days.inWholeSeconds
  SortType.TopWeek -> 7.days.inWholeSeconds
  SortType.TopMonth -> 30.days.inWholeSeconds
  SortType.TopYear -> 365.days.inWholeSeconds
  SortType.TopHour -> 1.hours.inWholeSeconds
  SortType.TopSixHour -> 6.hours.inWholeSeconds
  SortType.TopTwelveHour -> 12.hours.inWholeSeconds
  SortType.TopThreeMonths -> 3 * 30.days.inWholeSeconds
  SortType.TopSixMonths -> 6 * 30.days.inWholeSeconds
  SortType.TopNineMonths -> 9 * 30.days.inWholeSeconds
  SortType.TopAll -> null
  SortType.Active,
  SortType.Hot,
  SortType.New,
  SortType.Old,
  SortType.MostComments,
  SortType.NewComments,
  SortType.Controversial,
  SortType.Scaled,
  -> null
}

fun CommentSortTypeV3.toCommentSortType(): CommentSortType = when (this) {
  CommentSortTypeV3.Hot -> CommentSortType.hot
  CommentSortTypeV3.Top -> CommentSortType.top
  CommentSortTypeV3.New -> CommentSortType.new
  CommentSortTypeV3.Old -> CommentSortType.old
  CommentSortTypeV3.Controversial -> CommentSortType.controversial
}

fun SearchTypeV3.toSearchType(): SearchType? = when (this) {
  SearchTypeV3.All -> SearchType.all
  SearchTypeV3.Comments -> SearchType.comments
  SearchTypeV3.Posts -> SearchType.posts
  SearchTypeV3.Communities -> SearchType.communities
  SearchTypeV3.Users -> SearchType.users
  SearchTypeV3.Url -> null
}

fun ModlogActionType?.toModlogKindFilter(): ModlogKindFilter? = when (this) {
  ModlogActionType.All -> ModlogKindFilter.All
  ModlogActionType.ModRemovePost -> ModlogKindFilter.ModRemovePost
  ModlogActionType.ModLockPost -> ModlogKindFilter.ModLockPost
  ModlogActionType.ModFeaturePost -> ModlogKindFilter.ModFeaturePostCommunity
  ModlogActionType.ModRemoveComment -> ModlogKindFilter.ModRemoveComment
  ModlogActionType.ModRemoveCommunity -> ModlogKindFilter.AdminRemoveCommunity
  ModlogActionType.ModBanFromCommunity -> ModlogKindFilter.ModBanFromCommunity
  ModlogActionType.ModAddCommunity -> null
  ModlogActionType.ModTransferCommunity -> ModlogKindFilter.ModTransferCommunity
  ModlogActionType.ModAdd -> ModlogKindFilter.ModAddToCommunity
  ModlogActionType.ModBan -> ModlogKindFilter.ModBanFromCommunity
  ModlogActionType.ModHideCommunity -> ModlogKindFilter.ModChangeCommunityVisibility
  ModlogActionType.AdminPurgePerson -> ModlogKindFilter.AdminPurgePerson
  ModlogActionType.AdminPurgeCommunity -> ModlogKindFilter.AdminPurgeCommunity
  ModlogActionType.AdminPurgePost -> ModlogKindFilter.AdminPurgePost
  ModlogActionType.AdminPurgeComment -> ModlogKindFilter.AdminPurgeComment
  null -> null
}
