package com.idunnololz.summit.api.converters

import com.idunnololz.summit.api.dto.lemmy.Comment
import com.idunnololz.summit.api.dto.lemmy.CommentAggregates
import com.idunnololz.summit.api.dto.lemmy.CommentReply
import com.idunnololz.summit.api.dto.lemmy.CommentReplyView
import com.idunnololz.summit.api.dto.lemmy.CommentReport
import com.idunnololz.summit.api.dto.lemmy.CommentReportView
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.Community
import com.idunnololz.summit.api.dto.lemmy.CommunityAggregates
import com.idunnololz.summit.api.dto.lemmy.CommunityModeratorView
import com.idunnololz.summit.api.dto.lemmy.CommunityView
import com.idunnololz.summit.api.dto.lemmy.GetPostsResponse
import com.idunnololz.summit.api.dto.lemmy.GetSiteResponse
import com.idunnololz.summit.api.dto.lemmy.Language
import com.idunnololz.summit.api.dto.lemmy.ListingType
import com.idunnololz.summit.api.dto.lemmy.LocalSite
import com.idunnololz.summit.api.dto.lemmy.LocalSiteRateLimit
import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.api.dto.lemmy.PersonAggregates
import com.idunnololz.summit.api.dto.lemmy.PersonMention
import com.idunnololz.summit.api.dto.lemmy.PersonMentionView
import com.idunnololz.summit.api.dto.lemmy.PersonView
import com.idunnololz.summit.api.dto.lemmy.Post
import com.idunnololz.summit.api.dto.lemmy.PostAggregates
import com.idunnololz.summit.api.dto.lemmy.PostReport
import com.idunnololz.summit.api.dto.lemmy.PostReportView
import com.idunnololz.summit.api.dto.lemmy.PostView
import com.idunnololz.summit.api.dto.lemmy.PrivateMessage
import com.idunnololz.summit.api.dto.lemmy.PrivateMessageReport
import com.idunnololz.summit.api.dto.lemmy.PrivateMessageReportView
import com.idunnololz.summit.api.dto.lemmy.PrivateMessageView
import com.idunnololz.summit.api.dto.lemmy.RegistrationMode
import com.idunnololz.summit.api.dto.lemmy.Site
import com.idunnololz.summit.api.dto.lemmy.SiteAggregates
import com.idunnololz.summit.api.dto.lemmy.SiteView
import com.idunnololz.summit.api.dto.lemmy.SubscribedType
import com.idunnololz.summit.api.dto.lemmy.Tagline
import com.idunnololz.summit.api.dto.lemmy.VoteView
import com.idunnololz.summit.api.dto.lemmy.v4.models.Comment as CommentV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.CommentView as CommentViewV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.CommunityFollowerState
import com.idunnololz.summit.api.dto.lemmy.v4.models.CommunityModeratorView as CommunityModeratorViewV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.CommunityView as CommunityViewV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.Community as CommunityV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.FederationMode
import com.idunnololz.summit.api.dto.lemmy.v4.models.NotificationView
import com.idunnololz.summit.api.dto.lemmy.v4.models.PagedResponsePostView
import com.idunnololz.summit.api.dto.lemmy.v4.models.PostReportView as PostReportViewV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.ReportCombinedView
import com.idunnololz.summit.api.dto.lemmy.v4.models.VoteView as VoteViewV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.Post as PostV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.PostView as PostViewV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.Tagline as TaglineV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.Language as LanguageV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.Person as PersonV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.PersonView as PersonViewV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.LocalSiteRateLimit as LocalSiteRateLimitV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.RegistrationMode as RegistrationModeV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.ListingType as ListingTypeV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.LocalSite as LocalSiteV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.SiteView as SiteViewV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.Site as SiteV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.GetSiteResponse as GetSiteResponseV4
import com.idunnololz.summit.api.dto.lemmy.v4.models.PrivateMessage as PrivateMessageV4

fun GetSiteResponseV4.toGetSiteResponse(): GetSiteResponse {
  return GetSiteResponse(
    site_view = siteView.toSiteView(),
    admins = admins.map { it.toPersonView() },
    online = 0,
    version = version,
    my_user = null,
    all_languages = allLanguages.map { it.toLanguage() },
    discussion_languages = discussionLanguages.map { it.toInt() },
    taglines = if (tagline == null) {
      listOf()
    } else {
      listOf(tagline.toTagline())
    },
    custom_emojis = listOf(),
  )
}

fun PagedResponsePostView.getGetPostsResponse(): GetPostsResponse {
  return GetPostsResponse(
    this.items.map { it.toPostView() },
    this.nextPage,
    this.prevPage,
  )
}

fun SiteViewV4.toSiteView(): SiteView {
  return SiteView(
    site = site.toSite(),
    local_site = localSite.toLocalSite(),
    local_site_rate_limit = localSiteRateLimit.toLocalSiteRateLimit(),
    counts = localSite.toSiteAggregates(),
  )
}

fun SiteV4.toSite(): Site {
  return Site(
    id = id.toInt(),
    name = name,
    sidebar = sidebar,
    published = publishedAt,
    updated = updatedAt,
    icon = icon,
    banner = banner,
    description = summary,
    actor_id = apId,
    last_refreshed_at = lastRefreshedAt,
    inbox_url = inboxUrl,
    private_key = null,
    public_key = null,
    instance_id = instanceId.toInt(),
  )
}

fun LocalSiteV4.toLocalSite(): LocalSite = LocalSite(
  id = id.toInt(),
  site_id = siteId.toInt(),
  site_setup = siteSetup,
  enable_downvotes = postDownvotes != FederationMode.disable,
  enable_nsfw = !nsfwContentDisallowed,
  community_creation_admin_only = communityCreationAdminOnly,
  require_email_verification = emailVerificationRequired,
  application_question = applicationQuestion,
  private_instance = privateInstance,
  default_theme = defaultTheme,
  default_post_listing_type = defaultPostListingType.toListingType(),
  legal_information = legalInformation,
  hide_modlog_mod_names = false,
  application_email_admins = applicationEmailAdmins,
  slur_filter_regex = slurFilterRegex,
  actor_name_max_length = -1,
  federation_enabled = federationEnabled,
  federation_debug = false,
  federation_worker_count = 0,
  captcha_enabled = true,
  captcha_difficulty = null,
  published = publishedAt,
  updated = updatedAt,
  registration_mode = registrationMode.toRegistrationMode(),
  reports_email_admins = reportsEmailAdmins,
)

fun ListingTypeV4.toListingType(): ListingType =
  when (this) {
    ListingTypeV4.all -> ListingType.All
    ListingTypeV4.local -> ListingType.Local
    ListingTypeV4.subscribed -> ListingType.Subscribed
    ListingTypeV4.moderator_view -> ListingType.ModeratorView
    ListingTypeV4.suggested -> ListingType.All // TODO: Support suggested
  }

fun RegistrationModeV4.toRegistrationMode(): RegistrationMode =
  when (this) {
    RegistrationModeV4.closed -> RegistrationMode.Closed
    RegistrationModeV4.require_application -> RegistrationMode.RequireApplication
    RegistrationModeV4.open -> RegistrationMode.Open
    RegistrationModeV4.require_invitation -> RegistrationMode.RequireInvitation
  }

fun LocalSiteRateLimitV4.toLocalSiteRateLimit(): LocalSiteRateLimit {
  return LocalSiteRateLimit(
    id = 0,
    local_site_id = localSiteId.toInt(),
    message = messageMaxRequests.toInt(),
    message_per_second = messageIntervalSeconds.toInt(),
    post = postMaxRequests.toInt(),
    post_per_second = postIntervalSeconds.toInt(),
    register = registerMaxRequests.toInt(),
    register_per_second = registerIntervalSeconds.toInt(),
    image = imageMaxRequests.toInt(),
    image_per_second = imageIntervalSeconds.toInt(),
    comment = commentMaxRequests.toInt(),
    comment_per_second = commentIntervalSeconds.toInt(),
    search = searchMaxRequests.toInt(),
    search_per_second = searchIntervalSeconds.toInt(),
    published = publishedAt,
    updated = updatedAt,
  )
}

fun LocalSiteV4.toSiteAggregates(): SiteAggregates {
  return SiteAggregates(
    id.toInt(),
    siteId.toInt(),
    users.toInt(),
    posts.toInt(),
    comments.toInt(),
    communities.toInt(),
    usersActiveDay.toInt(),
    usersActiveWeek.toInt(),
    usersActiveMonth.toInt(),
    usersActiveHalfYear.toInt(),
  )
}

fun PersonViewV4.toPersonView(): PersonView =
  PersonView(
    this.toPerson(),
    this.toPersonAggregates(),
  )

fun PersonViewV4.toPerson(): Person =
  Person(
    id = person.id.toLong(),
    name = person.name,
    display_name = person.displayName,
    avatar = person.avatar,
    banned = banned,
    published = person.publishedAt,
    updated = person.updatedAt,
    actor_id = person.apId,
    bio = person.bio,
    local = person.local,
    banner = person.banner,
    deleted = person.deleted,
    matrix_user_id = person.matrixUserId,
    admin = isAdmin,
    bot_account = person.botAccount,
    ban_expires = banExpiresAt,
    instance_id = person.instanceId.toInt(),
  )

fun NotificationView.toPersonMentionView(): PersonMentionView {
  val d = data
  return PersonMentionView(
    person_mention = PersonMention(
      this.notification.id.toInt(),
      notification.recipientId.toLong(),
      notification.commentId?.toInt(),
      notification.read,
      notification.publishedAt,
    ),
    comment = d.comment.toComment(),
    creator = d.creator.toPerson(d.creatorBanned, d.creatorBanExpiresAt, d.creatorIsAdmin),
    post = d.post.toPost(),
    community = d.community.toCommunity(),
    recipient = d.recipient.toPerson(false, null, false),
    counts = null,
    creator_banned_from_community = d.creatorBannedFromCommunity,
    subscribed = SubscribedType.NotSubscribed,
    saved = d.postActions?.savedAt != null,
    creator_blocked = d.personActions?.blockedAt != null,
    my_vote = d.postActions?.voteIsUpvote?.toVoteInt(),
  )
}

fun NotificationView.toPrivateMessageView(): PrivateMessageView {
  val d = data
  return PrivateMessageView(
    private_message = d.privateMessage.toPrivateMessage(notification.read),
    creator = d.creator.toPerson(d.creatorBanned, d.creatorBanExpiresAt, d.creatorIsAdmin),
    recipient = d.recipient.toPerson(false, null, false)
  )
}

fun NotificationView.toCommentReplyView(): CommentReplyView {
  val d = data
  return CommentReplyView(
    comment_reply = CommentReply(
      id = notification.id,
      recipient_id = d.recipient.id,
      comment_id = d.comment.id,
      read = notification.read,
      published = notification.publishedAt,
    ),
    comment = d.comment.toComment(),
    creator = d.creator.toPerson(d.creatorBanned, d.creatorBanExpiresAt, d.creatorIsAdmin),
    post = d.post.toPost(),
    community = d.community.toCommunity(),
    recipient = d.recipient.toPerson(false, null, false),
    counts = d.comment.toCommentAggregates(),
    creator_banned_from_community = d.creatorBannedFromCommunity,
    subscribed = d.communityActions?.followState?.toSubscribedType(),
    saved = d.commentActions?.savedAt != null,
    creator_blocked = d.personActions?.blockedAt != null,
    my_vote = d.commentActions?.voteIsUpvote.toVoteInt(),
  )
}

private fun PersonViewV4.toPersonAggregates(): PersonAggregates {
  return PersonAggregates(
    id = person.id.toInt(),
    person_id = person.id,
    post_count = person.postCount.toInt(),
    comment_count = person.commentCount.toInt(),
  )
}

private fun LanguageV4.toLanguage(): Language {
  return Language(
    id = id.toInt(),
    code = code,
    name = name,
  )
}

private fun TaglineV4.toTagline(): Tagline {
  return Tagline(
    id.toInt(),
    0,
    content,
    publishedAt,
    updatedAt,
  )
}

fun PostViewV4.toPostView(): PostView {
  return PostView(
    post = post.toPost(),
    creator = creator.toPerson(creatorBanned, creatorBanExpiresAt, creatorIsAdmin),
    community = community.toCommunity(),
    creator_banned_from_community = creatorBannedFromCommunity,
    creator_is_moderator = creatorIsModerator,
    creator_is_admin = creatorIsAdmin,
    counts = post.toPostAggregates(),
    subscribed = communityActions?.followState?.toSubscribedType() ?: SubscribedType.NotSubscribed,
    saved = postActions?.savedAt != null,
    read = postActions?.readAt != null,
    creator_blocked = personActions?.blockedAt != null,
    my_vote = when (postActions?.voteIsUpvote) {
      true -> 1
      false -> -1
      null -> 0
    },
    unread_comments = post.comments.toInt() - (postActions?.readCommentsAmount?.toInt() ?: 0),
  )
}

fun CommunityViewV4.toCommunityView(): CommunityView {
  return CommunityView(
    community = community.toCommunity(),
    subscribed = communityActions?.followState?.toSubscribedType() ?: SubscribedType.NotSubscribed,
    blocked = communityActions?.blockedAt != null,
    counts = toCommunityAggregates(),
  )
}

fun CommentViewV4.toCommentView(): CommentView {
  return CommentView(
    comment = comment.toComment(),
    creator = creator.toPerson(creatorBanned, creatorBanExpiresAt, creatorIsAdmin),
    post = post.toPost(),
    community = community.toCommunity(),
    counts = comment.toCommentAggregates(),
    creator_banned_from_community = creatorBannedFromCommunity,
    creator_is_moderator = creatorIsModerator,
    creator_is_admin = creatorIsAdmin,
    subscribed = communityActions?.followState?.toSubscribedType() ?: SubscribedType.NotSubscribed,
    saved = commentActions?.savedAt != null,
    creator_blocked = personActions?.blockedAt != null,
    my_vote = when (commentActions?.voteIsUpvote) {
      true -> 1
      false -> -1
      null -> 0
    },
  )
}

fun VoteViewV4.toVoteView(): VoteView {
  return VoteView(
    creator = creator.toPerson(creatorBanned, null, false),
    creator_banned_from_community = creatorBannedFromCommunity,
    score = if (isUpvote) {
      1L
    } else {
      -1L
    },
  )
}

fun CommunityModeratorViewV4.toCommunityModeratorView(): CommunityModeratorView {
  return CommunityModeratorView(
    community.toCommunity(),
    moderator.toPerson(false, null, false),
  )
}

fun ReportCombinedView.toPrivateMessageReportView(): PrivateMessageReportView {
  val report = this.privateMessageReport

  return PrivateMessageReportView(
    private_message_report = PrivateMessageReport(
      id = report.id,
      creator_id = report.creatorId,
      private_message_id = report.privateMessageId,
      original_pm_text = report.originalPmText,
      reason = report.reason,
      resolved = report.resolved,
      resolver_id = report.resolverId,
      published = report.publishedAt,
      updated = report.updatedAt,
    ),
    private_message = privateMessage.toPrivateMessage(read = false),
    private_message_creator = privateMessageCreator.toPerson(
      isBanned = false, banExpires = null, isAdmin = false
    ),
    creator = creator.toPerson(
      isBanned = creatorBanned,
      banExpires = creatorBanExpiresAt,
      isAdmin = creatorIsAdmin,
    ),
    resolver = resolver?.toPerson(
      isBanned = false, banExpires = null, isAdmin = false
    )
  )
}

fun ReportCombinedView.toPostReportView(): PostReportView {
  return PostReportView(
    post_report = PostReport(
      id = postReport.id,
      creator_id = postReport.creatorId,
      post_id = postReport.postId,
      original_post_name = postReport.originalPostName,
      original_post_url = postReport.originalPostUrl,
      original_post_body = postReport.originalPostBody,
      reason = postReport.reason,
      resolved = postReport.resolved,
      resolver_id = postReport.resolverId,
      published = postReport.publishedAt,
      updated = postReport.updatedAt,
    ),
    post = post.toPost(),
    community = community.toCommunity(),
    creator = creator.toPerson(
      isBanned = creatorBanned,
      banExpires = creatorBanExpiresAt,
      isAdmin = creatorIsAdmin,
    ),
    post_creator = postCreator.toPerson(
      isBanned = false,
      banExpires = null,
      isAdmin = false,
    ),
    creator_banned_from_community = creatorBannedFromCommunity,
    my_vote = this.postActions?.voteIsUpvote?.toVoteInt(),
    counts = this.post.toPostAggregates(),
    resolver = this.resolver?.toPerson(
      isBanned = false,
      banExpires = null,
      isAdmin = false,
    ),
  )
}

fun ReportCombinedView.toCommentReportView(): CommentReportView {
  return CommentReportView(
    comment_report = CommentReport(
      id = commentReport.id,
      creator_id = commentReport.creatorId,
      comment_id = commentReport.commentId,
      original_comment_text = commentReport.originalCommentText,
      reason = commentReport.reason,
      resolved = commentReport.resolved,
      resolver_id = commentReport.resolverId,
      published = commentReport.publishedAt,
      updated = commentReport.updatedAt,
    ),
    comment = comment.toComment(),
    post = post.toPost(),
    community = community.toCommunity(),
    creator = creator.toPerson(
      isBanned = creatorBanned,
      banExpires = creatorBanExpiresAt,
      isAdmin = creatorIsAdmin,
    ),
    comment_creator = commentCreator.toPerson(
      isBanned = false,
      banExpires = null,
      isAdmin = false,
    ),
    counts = comment.toCommentAggregates(),
    creator_banned_from_community = creatorBannedFromCommunity,
    my_vote = commentActions?.voteIsUpvote?.toVoteInt(),
    resolver = this.resolver?.toPerson(
      isBanned = false,
      banExpires = null,
      isAdmin = false,
    ),
  )
}

private fun CommentV4.toCommentAggregates(): CommentAggregates {
  return CommentAggregates(
    id = id,
    comment_id = id,
    score = score.toInt(),
    upvotes = upvotes.toInt(),
    downvotes = downvotes.toInt(),
    published = publishedAt,
    child_count = childCount.toInt(),
  )
}

private fun CommentV4.toComment(): Comment {
  return Comment(
    id = id.toInt(),
    creator_id = creatorId.toLong(),
    post_id = postId.toInt(),
    content = content,
    removed = removed,
    published = publishedAt,
    updated = updatedAt,
    deleted = deleted,
    ap_id = apId,
    local = local,
    path = path,
    distinguished = distinguished,
    language_id = languageId.toInt(),
  )
}

private fun CommunityViewV4.toCommunityAggregates(): CommunityAggregates {
  return CommunityAggregates(
    this.community.id.toInt(),
    this.community.id.toInt(),
    this.community.subscribers.toInt(),
    this.community.posts.toInt(),
    this.community.comments.toInt(),
    this.community.publishedAt,
    this.community.usersActiveDay.toInt(),
    this.community.usersActiveWeek.toInt(),
    this.community.usersActiveMonth.toInt(),
    this.community.usersActiveHalfYear.toInt(),
  )
}

private fun CommunityFollowerState.toSubscribedType(): SubscribedType {
  return when (this) {
    CommunityFollowerState.accepted -> SubscribedType.Subscribed
    CommunityFollowerState.pending -> SubscribedType.Pending
    CommunityFollowerState.approval_required,
    CommunityFollowerState.denied -> SubscribedType.NotSubscribed
  }
}

private fun PostV4.toPostAggregates(): PostAggregates {
  return PostAggregates(
    id = id.toInt(),
    post_id = id.toInt(),
    comments = comments.toInt(),
    score = score.toInt(),
    upvotes = upvotes.toInt(),
    downvotes = downvotes.toInt(),
    published = publishedAt,
    newest_comment_time_necro = null,
    newest_comment_time = newestCommentTimeAt,
    featured_community = featuredCommunity,
    featured_local = featuredLocal,
    hot_rank = null,
    hot_rank_active = null,
    scaled_rank = null,
    controversy_rank = null,
  )
}

private fun PostV4.toPost(): Post {
  return Post(
    id = id.toInt(),
    name = name,
    url = url,
    body = body,
    creator_id = creatorId.toLong(),
    community_id = communityId.toInt(),
    removed = removed,
    locked = locked,
    published = publishedAt,
    updated = updatedAt,
    deleted = deleted,
    nsfw = nsfw,
    embed_title = embedTitle,
    embed_description = embedDescription,
    thumbnail_url = thumbnailUrl,
    ap_id = apId,
    local = local,
    embed_video_url = embedVideoUrl,
    language_id = languageId.toInt(),
    featured_community = featuredCommunity,
    featured_local = featuredLocal,
    url_content_type = urlContentType,
    alt_text = altText,
  )
}

private fun PersonV4.toPerson(
  isBanned: Boolean,
  banExpires: String?,
  isAdmin: Boolean,
): Person {
  return Person(
    id = id.toLong(),
    name = name,
    display_name = displayName,
    avatar = avatar,
    banned = false,
    published = publishedAt,
    updated = updatedAt,
    actor_id = apId,
    bio = bio,
    local = local,
    banner = banner,
    deleted = deleted,
    matrix_user_id = matrixUserId,
    admin = false,
    bot_account = botAccount,
    ban_expires = null,
    instance_id = instanceId.toInt(),
  )
}

private fun CommunityV4.toCommunity(): Community = Community(
  id = id.toInt(),
  name = name,
  title = title,
  description = summary,
  removed = removed,
  published = publishedAt,
  updated = updatedAt,
  deleted = deleted,
  nsfw = nsfw,
  actor_id = apId,
  local = local,
  icon = icon,
  banner = banner,
  hidden = false,
  posting_restricted_to_mods = postingRestrictedToMods,
  instance_id = instanceId.toInt(),
)

fun PrivateMessageV4.toPrivateMessage(read: Boolean): PrivateMessage =
  PrivateMessage(
    id = id,
    creator_id = creatorId,
    recipient_id = recipientId,
    content = content,
    deleted = deleted,
    read = read,
    published = publishedAt,
    updated = updatedAt,
    ap_id = apId,
    local = local,
  )

private fun Boolean?.toVoteInt() =
  when (this) {
    true -> 1
    false -> -1
    null -> 0
  }