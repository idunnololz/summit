package com.idunnololz.summit.api.converters

import com.idunnololz.summit.api.dto.lemmy.CreateComment
import com.idunnololz.summit.api.dto.lemmy.CreatePost
import com.idunnololz.summit.api.dto.lemmy.EditComment
import com.idunnololz.summit.api.dto.lemmy.EditPost
import com.idunnololz.summit.api.dto.lemmy.RegistrationApplicationView
import com.idunnololz.summit.api.dto.lemmy.SortType
import com.idunnololz.summit.api.dto.piefed.SortApiAlphaUserGet
import com.idunnololz.summit.api.dto.piefed.models.CreateCommentRequest
import com.idunnololz.summit.api.dto.piefed.models.CreatePostRequest
import com.idunnololz.summit.api.dto.piefed.models.EditCommentRequest
import com.idunnololz.summit.api.dto.piefed.models.EditPostRequest
import com.idunnololz.summit.api.local.UserRegistrationApplication

fun CreateComment.toCreateComment() = CreateCommentRequest(
  body = this.content,
  postId = this.post_id,
  parentId = this.parent_id,
  languageId = this.language_id,
)

fun CreatePost.toCreatePost() = CreatePostRequest(
  title = this.name,
  communityId = this.community_id,
  url = this.url,
  body = this.body,
  nsfw = this.nsfw,
  languageId = this.language_id,
)

fun SortType.toSortType() = when (this) {
  SortType.Active -> SortApiAlphaUserGet.Active
  SortType.Hot -> SortApiAlphaUserGet.Hot
  SortType.New -> SortApiAlphaUserGet.New
  SortType.Old -> SortApiAlphaUserGet.Hot
  SortType.TopDay -> SortApiAlphaUserGet.TopDay
  SortType.TopWeek -> SortApiAlphaUserGet.TopWeek
  SortType.TopMonth -> SortApiAlphaUserGet.TopMonth
  SortType.TopYear -> SortApiAlphaUserGet.TopYear
  SortType.TopAll -> SortApiAlphaUserGet.TopAll
  SortType.MostComments -> SortApiAlphaUserGet.Hot
  SortType.NewComments -> SortApiAlphaUserGet.Hot
  SortType.TopHour -> SortApiAlphaUserGet.TopHour
  SortType.TopSixHour -> SortApiAlphaUserGet.TopSixHour
  SortType.TopTwelveHour -> SortApiAlphaUserGet.TopTwelveHour
  SortType.TopThreeMonths -> SortApiAlphaUserGet.TopThreeMonths
  SortType.TopSixMonths -> SortApiAlphaUserGet.TopSixMonths
  SortType.TopNineMonths -> SortApiAlphaUserGet.TopNineMonths
  SortType.Controversial -> SortApiAlphaUserGet.Hot
  SortType.Scaled -> SortApiAlphaUserGet.Scaled
}

internal fun EditComment.toEditComment() =
  EditCommentRequest(
    commentId = this.comment_id,
    body = this.content ?: "",
    languageId = this.language_id,
  )

internal fun EditPost.toEditPost() =
  EditPostRequest(
    postId = this.post_id,
    title = this.name,
    url = this.url,
    body = this.body,
    nsfw = this.nsfw,
    languageId = this.language_id,
  )

internal fun RegistrationApplicationView.toUserRegistrationApplication(instance: String) =
  UserRegistrationApplication(
    id = this.registration_application.id,
    answer = this.registration_application.answer,
    email = null,
    ipAddress = null,
    userId = this.registration_application.id.toLong(),
    userName = this.creator.name,
    instance = instance,
    isRead = this.registration_application.deny_reason != null || admin != null,
    status = if (this.admin != null) {
      if (this.creator_local_user.accepted_application) {
        UserRegistrationApplication.Status.Approved
      } else {
        UserRegistrationApplication.Status.Declined
      }
    } else {
      UserRegistrationApplication.Status.NoDecision
    },
    appliedAt = this.registration_application.published,
    countryCode = null,
    throwawayEmail = null,
    approvedBy = this.admin,
    approvedAt = null,
    referrer = null,
    denyReason = this.registration_application.deny_reason,
  )