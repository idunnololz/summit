package com.idunnololz.summit.api.converters

import com.idunnololz.summit.api.dto.lemmy.ListingType
import com.idunnololz.summit.api.dto.lemmy.SortType
import com.idunnololz.summit.api.dto.lemmy.v4.models.PostSortType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

fun ListingType.toType() =
  when (this) {
    ListingType.All ->
      com.idunnololz.summit.api.dto.lemmy.v4.models.ListingType.all
    ListingType.Local ->
      com.idunnololz.summit.api.dto.lemmy.v4.models.ListingType.local
    ListingType.Subscribed ->
      com.idunnololz.summit.api.dto.lemmy.v4.models.ListingType.subscribed
    ListingType.ModeratorView ->
      com.idunnololz.summit.api.dto.lemmy.v4.models.ListingType.moderator_view
  }

fun SortType.toPostSortType() =
  when (this) {
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
    SortType.TopAll -> PostSortType.top
    SortType.MostComments -> PostSortType.most_comments
    SortType.NewComments -> PostSortType.new_comments
    SortType.Controversial -> PostSortType.controversial
    SortType.Scaled -> PostSortType.scaled
  }

fun SortType.toTimeInSeconds(): Long? =
  when (this) {
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
    SortType.Scaled -> null
  }