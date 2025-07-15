package com.idunnololz.summit.api.converters

import com.idunnololz.summit.api.dto.piefed.CreateComment
import com.idunnololz.summit.api.dto.piefed.CreatePost
import com.idunnololz.summit.api.dto.piefed.SortType

fun com.idunnololz.summit.api.dto.lemmy.CreateComment.toCreateComment() = CreateComment(
  this.content,
  this.post_id,
  this.parent_id,
  this.language_id,
)

fun com.idunnololz.summit.api.dto.lemmy.CreatePost.toCreatePost() = CreatePost(
  this.name,
  this.community_id,
  this.url,
  this.body,
  this.nsfw,
  this.language_id,
)

fun com.idunnololz.summit.api.dto.lemmy.SortType.toSortType() = when (this) {
  com.idunnololz.summit.api.dto.lemmy.SortType.Active -> SortType.Active
  com.idunnololz.summit.api.dto.lemmy.SortType.Hot -> SortType.Hot
  com.idunnololz.summit.api.dto.lemmy.SortType.New -> SortType.New
  com.idunnololz.summit.api.dto.lemmy.SortType.Old -> SortType.Hot
  com.idunnololz.summit.api.dto.lemmy.SortType.TopDay -> SortType.TopDay
  com.idunnololz.summit.api.dto.lemmy.SortType.TopWeek -> SortType.TopWeek
  com.idunnololz.summit.api.dto.lemmy.SortType.TopMonth -> SortType.TopMonth
  com.idunnololz.summit.api.dto.lemmy.SortType.TopYear -> SortType.TopYear
  com.idunnololz.summit.api.dto.lemmy.SortType.TopAll -> SortType.TopAll
  com.idunnololz.summit.api.dto.lemmy.SortType.MostComments -> SortType.Hot
  com.idunnololz.summit.api.dto.lemmy.SortType.NewComments -> SortType.Hot
  com.idunnololz.summit.api.dto.lemmy.SortType.TopHour -> SortType.TopHour
  com.idunnololz.summit.api.dto.lemmy.SortType.TopSixHour -> SortType.TopSixHour
  com.idunnololz.summit.api.dto.lemmy.SortType.TopTwelveHour -> SortType.TopTwelveHour
  com.idunnololz.summit.api.dto.lemmy.SortType.TopThreeMonths -> SortType.TopThreeMonths
  com.idunnololz.summit.api.dto.lemmy.SortType.TopSixMonths -> SortType.TopSixMonths
  com.idunnololz.summit.api.dto.lemmy.SortType.TopNineMonths -> SortType.TopNineMonths
  com.idunnololz.summit.api.dto.lemmy.SortType.Controversial -> SortType.Hot
  com.idunnololz.summit.api.dto.lemmy.SortType.Scaled -> SortType.Scaled
}
