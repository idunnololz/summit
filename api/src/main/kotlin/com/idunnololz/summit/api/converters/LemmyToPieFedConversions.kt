package com.idunnololz.summit.api.converters

import com.idunnololz.summit.api.dto.lemmy.EditComment
import com.idunnololz.summit.api.dto.lemmy.EditPost
import com.idunnololz.summit.api.dto.piefed.SortApiAlphaUserGet
import com.idunnololz.summit.api.dto.piefed.models.CreateCommentRequest
import com.idunnololz.summit.api.dto.piefed.models.CreatePostRequest

fun com.idunnololz.summit.api.dto.lemmy.CreateComment.toCreateComment() = CreateCommentRequest(
  body = this.content,
  postId = this.post_id,
  parentId = this.parent_id,
  languageId = this.language_id,
)

fun com.idunnololz.summit.api.dto.lemmy.CreatePost.toCreatePost() = CreatePostRequest(
  title = this.name,
  communityId = this.community_id,
  url = this.url,
  body = this.body,
  nsfw = this.nsfw,
  languageId = this.language_id,
)

fun com.idunnololz.summit.api.dto.lemmy.SortType.toSortType() = when (this) {
  com.idunnololz.summit.api.dto.lemmy.SortType.Active -> SortApiAlphaUserGet.Active
  com.idunnololz.summit.api.dto.lemmy.SortType.Hot -> SortApiAlphaUserGet.Hot
  com.idunnololz.summit.api.dto.lemmy.SortType.New -> SortApiAlphaUserGet.New
  com.idunnololz.summit.api.dto.lemmy.SortType.Old -> SortApiAlphaUserGet.Hot
  com.idunnololz.summit.api.dto.lemmy.SortType.TopDay -> SortApiAlphaUserGet.TopDay
  com.idunnololz.summit.api.dto.lemmy.SortType.TopWeek -> SortApiAlphaUserGet.TopWeek
  com.idunnololz.summit.api.dto.lemmy.SortType.TopMonth -> SortApiAlphaUserGet.TopMonth
  com.idunnololz.summit.api.dto.lemmy.SortType.TopYear -> SortApiAlphaUserGet.TopYear
  com.idunnololz.summit.api.dto.lemmy.SortType.TopAll -> SortApiAlphaUserGet.TopAll
  com.idunnololz.summit.api.dto.lemmy.SortType.MostComments -> SortApiAlphaUserGet.Hot
  com.idunnololz.summit.api.dto.lemmy.SortType.NewComments -> SortApiAlphaUserGet.Hot
  com.idunnololz.summit.api.dto.lemmy.SortType.TopHour -> SortApiAlphaUserGet.TopHour
  com.idunnololz.summit.api.dto.lemmy.SortType.TopSixHour -> SortApiAlphaUserGet.TopSixHour
  com.idunnololz.summit.api.dto.lemmy.SortType.TopTwelveHour -> SortApiAlphaUserGet.TopTwelveHour
  com.idunnololz.summit.api.dto.lemmy.SortType.TopThreeMonths -> SortApiAlphaUserGet.TopThreeMonths
  com.idunnololz.summit.api.dto.lemmy.SortType.TopSixMonths -> SortApiAlphaUserGet.TopSixMonths
  com.idunnololz.summit.api.dto.lemmy.SortType.TopNineMonths -> SortApiAlphaUserGet.TopNineMonths
  com.idunnololz.summit.api.dto.lemmy.SortType.Controversial -> SortApiAlphaUserGet.Hot
  com.idunnololz.summit.api.dto.lemmy.SortType.Scaled -> SortApiAlphaUserGet.Scaled
}

internal fun EditComment.toEditComment() =
  com.idunnololz.summit.api.dto.piefed.models.EditCommentRequest(
    commentId = this.comment_id,
    body = this.content ?: "",
    languageId = this.language_id,
  )

internal fun EditPost.toEditPost() =
  com.idunnololz.summit.api.dto.piefed.models.EditPostRequest(
    postId = this.post_id,
    title = this.name,
    url = this.url,
    body = this.body,
    nsfw = this.nsfw,
    languageId = this.language_id,
  )
