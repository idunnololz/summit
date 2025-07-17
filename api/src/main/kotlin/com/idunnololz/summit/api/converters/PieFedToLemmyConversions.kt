package com.idunnololz.summit.api.converters

import com.idunnololz.summit.api.dto.lemmy.CommentReplyView
import com.idunnololz.summit.api.dto.lemmy.CommentReport
import com.idunnololz.summit.api.dto.lemmy.CommentReportView
import com.idunnololz.summit.api.dto.lemmy.CommentResponse
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.CommunityAggregates
import com.idunnololz.summit.api.dto.lemmy.CommunityModeratorView
import com.idunnololz.summit.api.dto.lemmy.CommunityResponse
import com.idunnololz.summit.api.dto.lemmy.GetPersonMentionsResponse
import com.idunnololz.summit.api.dto.lemmy.GetRepliesResponse
import com.idunnololz.summit.api.dto.lemmy.LoginResponse
import com.idunnololz.summit.api.dto.lemmy.PersonMention
import com.idunnololz.summit.api.dto.lemmy.PersonMentionView
import com.idunnololz.summit.api.dto.lemmy.PersonView
import com.idunnololz.summit.api.dto.lemmy.PostReport
import com.idunnololz.summit.api.dto.lemmy.PostReportView
import com.idunnololz.summit.api.dto.lemmy.PostResponse
import com.idunnololz.summit.api.dto.lemmy.PostView
import com.idunnololz.summit.api.dto.lemmy.PrivateMessage
import com.idunnololz.summit.api.dto.lemmy.PrivateMessageView
import com.idunnololz.summit.api.dto.lemmy.SearchType
import com.idunnololz.summit.api.dto.piefed.Comment
import com.idunnololz.summit.api.dto.piefed.CommentAggregates
import com.idunnololz.summit.api.dto.piefed.CommentReply
import com.idunnololz.summit.api.dto.piefed.Community
import com.idunnololz.summit.api.dto.piefed.CommunityBlockView
import com.idunnololz.summit.api.dto.piefed.CommunityFollowerView
import com.idunnololz.summit.api.dto.piefed.CommunityView
import com.idunnololz.summit.api.dto.piefed.Instance
import com.idunnololz.summit.api.dto.piefed.InstanceBlockView
import com.idunnololz.summit.api.dto.piefed.ListingType
import com.idunnololz.summit.api.dto.piefed.LocalUser
import com.idunnololz.summit.api.dto.piefed.LocalUserView
import com.idunnololz.summit.api.dto.piefed.MyUserInfo
import com.idunnololz.summit.api.dto.piefed.Person
import com.idunnololz.summit.api.dto.piefed.PersonAggregates
import com.idunnololz.summit.api.dto.piefed.PersonBlockView
import com.idunnololz.summit.api.dto.piefed.Post
import com.idunnololz.summit.api.dto.piefed.PostAggregates
import com.idunnololz.summit.api.dto.piefed.Site
import com.idunnololz.summit.api.dto.piefed.SortType
import com.idunnololz.summit.api.dto.piefed.SubscribedType

internal fun com.idunnololz.summit.api.dto.piefed.PersonView.toPersonView(): PersonView =
  PersonView(
    person.toPerson(),
    counts.toPersonAggregates(),
  )

private fun Person.toPerson(): com.idunnololz.summit.api.dto.lemmy.Person =
  com.idunnololz.summit.api.dto.lemmy.Person(
    id = this.id.toLong(),
    name = this.userName,
    display_name = null,
    avatar = this.avatar,
    banned = this.banned,
    published = this.published,
    updated = null,
    actor_id = this.actorId,
    bio = null,
    local = this.local,
    banner = this.banner,
    deleted = this.deleted,
    matrix_user_id = null,
    admin = false,
    bot_account = this.bot,
    ban_expires = null,
    instance_id = this.instanceId,
  )

private fun PersonAggregates.toPersonAggregates(): com.idunnololz.summit.api.dto.lemmy.PersonAggregates =
  com.idunnololz.summit.api.dto.lemmy.PersonAggregates(
    id = this.personId,
    person_id = this.personId.toLong(),
    post_count = this.postCount,
    comment_count = this.commentCount,
  )

internal fun com.idunnololz.summit.api.dto.piefed.CommentView.toCommentView(): CommentView =
  CommentView(
    comment = this.comment.toComment(),
    creator = this.creator.toPerson(),
    post = this.post.toPost(),
    community = this.community.toCommunity(),
    counts = this.counts.toCommentAggregates(),
    creator_banned_from_community = this.creatorBannedFromCommunity,
    creator_is_moderator = this.creatorIsModerator,
    creator_is_admin = this.creatorIsAdmin,
    subscribed = this.subscribed.toSubscribedType(),
    saved = this.saved,
    creator_blocked = this.creatorBlocked,
    my_vote = this.myVote,
  )

internal fun Comment.toComment(): com.idunnololz.summit.api.dto.lemmy.Comment =
  com.idunnololz.summit.api.dto.lemmy.Comment(
    id = this.id,
    creator_id = this.userId.toLong(),
    post_id = this.postId,
    content = this.body,
    removed = this.removed,
    published = this.published,
    updated = this.updated ?: this.editedAt,
    deleted = this.deleted,
    ap_id = this.apId,
    local = this.local,
    path = this.path,
    distinguished = this.distinguished ?: false,
    language_id = this.languageId,
  )

internal fun Community.toCommunity(): com.idunnololz.summit.api.dto.lemmy.Community =
  com.idunnololz.summit.api.dto.lemmy.Community(
    id = this.id,
    name = this.name,
    title = this.title,
    description = this.description,
    removed = this.removed,
    published = this.published,
    updated = this.updated,
    deleted = this.deleted,
    nsfw = this.nsfw,
    actor_id = this.actorId,
    local = this.local,
    icon = this.icon,
    banner = this.banner,
    hidden = this.hidden,
    posting_restricted_to_mods = false,
    instance_id = this.instanceId,
  )

internal fun SubscribedType.toSubscribedType() = when (this) {
  SubscribedType.Subscribed -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.Subscribed
  SubscribedType.NotSubscribed -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.NotSubscribed
  SubscribedType.Pending -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.Pending
}

internal fun Post.toPost(): com.idunnololz.summit.api.dto.lemmy.Post =
  com.idunnololz.summit.api.dto.lemmy.Post(
    id = this.id,
    name = this.title,
    url = this.url,
    body = this.body,
    creator_id = this.userId.toLong(),
    community_id = this.communityId,
    removed = this.removed,
    locked = this.locked,
    published = this.published,
    updated = this.updated ?: this.editedAt,
    deleted = this.deleted,
    nsfw = this.nsfw,
    embed_title = null,
    embed_description = null,
    thumbnail_url = this.thumbnailUrl,
    ap_id = this.apId,
    local = this.local,
    embed_video_url = null,
    language_id = this.languageId,
    featured_community = false,
    featured_local = false,
    url_content_type = null,
    alt_text = this.altText,
  )

internal fun CommentAggregates.toCommentAggregates(): com.idunnololz.summit.api.dto.lemmy.CommentAggregates =
  com.idunnololz.summit.api.dto.lemmy.CommentAggregates(
    id = this.commentId,
    comment_id = this.commentId,
    score = this.score,
    upvotes = this.upvotes,
    downvotes = this.downvotes,
    published = this.published,
    child_count = this.childCount,
  )

internal fun com.idunnololz.summit.api.dto.piefed.PostView.toPostView(): PostView = PostView(
  post = this.post.toPost(),
  creator = this.creator.toPerson(),
  community = this.community.toCommunity(),
  creator_banned_from_community = this.creatorBannedFromCommunity,
  creator_is_moderator = this.creatorIsModerator,
  creator_is_admin = this.creatorIsAdmin,
  counts = this.counts.toPostAggregates(),
  subscribed = this.subscribed.toSubscribedType(),
  saved = this.saved,
  read = this.read,
  creator_blocked = false,
  my_vote = this.myVote,
  unread_comments = this.unreadComments,
)

internal fun PostAggregates.toPostAggregates(): com.idunnololz.summit.api.dto.lemmy.PostAggregates =
  com.idunnololz.summit.api.dto.lemmy.PostAggregates(
    id = this.postId,
    post_id = this.postId,
    comments = this.comments,
    score = this.score,
    upvotes = this.upvotes,
    downvotes = this.downvotes,
    published = this.published,
    newest_comment_time_necro = null,
    newest_comment_time = this.newestCommentTime,
    featured_community = false,
    featured_local = false,
    hot_rank = null,
    hot_rank_active = null,
    scaled_rank = null,
    controversy_rank = null,
  )

internal fun com.idunnololz.summit.api.dto.piefed.CommunityModeratorView.toCommunityModeratorView() =
  CommunityModeratorView(
    community = this.community.toCommunity(),
    moderator = this.moderator.toPerson(),
  )

internal fun Site.toSite(): com.idunnololz.summit.api.dto.lemmy.Site =
  com.idunnololz.summit.api.dto.lemmy.Site(
    id = 1,
    name = this.name,
    sidebar = this.sidebar,
    published = null,
    updated = null,
    icon = this.icon,
    banner = null,
    description = this.description,
    actor_id = this.actorId,
    last_refreshed_at = null,
    inbox_url = null,
    private_key = null,
    public_key = null,
    instance_id = null,
  )

internal fun MyUserInfo.toMyUserInfo(): com.idunnololz.summit.api.dto.lemmy.MyUserInfo =
  com.idunnololz.summit.api.dto.lemmy.MyUserInfo(
    this.localUserView.toLocalUserView(),
    this.follows.map { it.toCommunityFollowerView() },
    this.moderates.map { it.toCommunityModeratorView() },
    this.communityBlocks.map { it.toCommunityBlockView() },
    this.personBlocks.map { it.toPersonBlockView() },
    this.instanceBlocks.map { it.toInstanceBlockView() },
    this.discussionLanguages.mapNotNull { it.id },
  )

internal fun CommunityFollowerView.toCommunityFollowerView(): com.idunnololz.summit.api.dto.lemmy.CommunityFollowerView =
  com.idunnololz.summit.api.dto.lemmy.CommunityFollowerView(
    community = this.community.toCommunity(),
    follower = this.follower.toPerson(),
  )

internal fun CommunityBlockView.toCommunityBlockView(): com.idunnololz.summit.api.dto.lemmy.CommunityBlockView =
  com.idunnololz.summit.api.dto.lemmy.CommunityBlockView(
    person = this.person.toPerson(),
    community = this.community.toCommunity(),
  )

internal fun PersonBlockView.toPersonBlockView(): com.idunnololz.summit.api.dto.lemmy.PersonBlockView =
  com.idunnololz.summit.api.dto.lemmy.PersonBlockView(
    person = this.person.toPerson(),
    target = this.target.toPerson(),
  )

internal fun InstanceBlockView.toInstanceBlockView(): com.idunnololz.summit.api.dto.lemmy.InstanceBlockView =
  com.idunnololz.summit.api.dto.lemmy.InstanceBlockView(
    person = this.person.toPerson(),
    instance = this.instance.toInstance(),
    site = this.site?.toSite(),
  )

internal fun Instance.toInstance(): com.idunnololz.summit.api.dto.lemmy.Instance =
  com.idunnololz.summit.api.dto.lemmy.Instance(
    id = this.id,
    domain = this.domain,
    published = this.published,
    updated = this.updated,
    software = this.software,
    version = this.version,
  )

internal fun LocalUserView.toLocalUserView(): com.idunnololz.summit.api.dto.lemmy.LocalUserView =
  com.idunnololz.summit.api.dto.lemmy.LocalUserView(
    this.localUser.toLocalUser().copy(
      id = this.person.id,
      person_id = this.person.id.toLong(),
    ),
    this.person.toPerson(),
    this.counts.toPersonAggregates(),
  )

internal fun LocalUser.toLocalUser(): com.idunnololz.summit.api.dto.lemmy.LocalUser =
  com.idunnololz.summit.api.dto.lemmy.LocalUser(
    id = 0,
    person_id = 0,
    email = null,
    show_nsfw = this.showNsfw,
    theme = "",
    default_sort_type = this.defaultSortType.toSortType(),
    default_listing_type = this.defaultListingType.toListingType(),
    interface_language = "",
    show_avatars = false,
    send_notifications_to_email = false,
    validator_time = "",
    show_scores = this.showScores,
    show_bot_accounts = this.showBotAccounts,
    show_read_posts = this.showReadPosts,
    show_new_post_notifs = false,
    email_verified = true,
    accepted_application = true,
    totp_2fa_url = null,
  )

internal fun SortType.toSortType(): com.idunnololz.summit.api.dto.lemmy.SortType? = when (this) {
  SortType.Active -> com.idunnololz.summit.api.dto.lemmy.SortType.Active
  SortType.Hot -> com.idunnololz.summit.api.dto.lemmy.SortType.Hot
  SortType.New -> com.idunnololz.summit.api.dto.lemmy.SortType.New
  SortType.TopHour -> com.idunnololz.summit.api.dto.lemmy.SortType.TopHour
  SortType.TopSixHour -> com.idunnololz.summit.api.dto.lemmy.SortType.TopSixHour
  SortType.TopTwelveHour -> com.idunnololz.summit.api.dto.lemmy.SortType.TopTwelveHour
  SortType.TopDay -> com.idunnololz.summit.api.dto.lemmy.SortType.TopDay
  SortType.TopWeek -> com.idunnololz.summit.api.dto.lemmy.SortType.TopWeek
  SortType.TopMonth -> com.idunnololz.summit.api.dto.lemmy.SortType.TopMonth
  SortType.TopThreeMonths -> com.idunnololz.summit.api.dto.lemmy.SortType.TopThreeMonths
  SortType.TopSixMonths -> com.idunnololz.summit.api.dto.lemmy.SortType.TopSixMonths
  SortType.TopNineMonths -> com.idunnololz.summit.api.dto.lemmy.SortType.TopNineMonths
  SortType.TopYear -> com.idunnololz.summit.api.dto.lemmy.SortType.TopYear
  SortType.TopAll -> com.idunnololz.summit.api.dto.lemmy.SortType.TopAll
  SortType.Scaled -> com.idunnololz.summit.api.dto.lemmy.SortType.Scaled
}

internal fun ListingType.toListingType(): com.idunnololz.summit.api.dto.lemmy.ListingType =
  when (this) {
    ListingType.All -> com.idunnololz.summit.api.dto.lemmy.ListingType.All
    ListingType.Local -> com.idunnololz.summit.api.dto.lemmy.ListingType.Local
    ListingType.Subscribed -> com.idunnololz.summit.api.dto.lemmy.ListingType.Subscribed
    ListingType.Popular -> com.idunnololz.summit.api.dto.lemmy.ListingType.All
    ListingType.ModeratorView -> com.idunnololz.summit.api.dto.lemmy.ListingType.ModeratorView
  }

internal fun CommunityView.toCommunityView(): com.idunnololz.summit.api.dto.lemmy.CommunityView =
  com.idunnololz.summit.api.dto.lemmy.CommunityView(
    community = this.community.toCommunity(),
    subscribed = this.subscribed.toSubscribedType(),
    blocked = this.blocked,
    counts = this.counts.toCommunityAggregates(),
  )

internal fun com.idunnololz.summit.api.dto.piefed.CommunityAggregates.toCommunityAggregates(): CommunityAggregates =
  CommunityAggregates(
    id = this.id,
    community_id = this.id,
    subscribers = this.subscriptionsCount,
    posts = this.postCount,
    comments = this.postReplyCount,
    published = this.published,
    users_active_day = this.activeDaily,
    users_active_week = this.activeWeekly,
    users_active_month = this.activeMonthly,
    users_active_half_year = this.active6monthly,
  )

internal fun com.idunnololz.summit.api.dto.piefed.PostResponse.toPostResponse() = PostResponse(
  this.postView.toPostView(),
)

internal fun com.idunnololz.summit.api.dto.piefed.CommentResponse.toCommentResponse() =
  CommentResponse(
    this.commentView.toCommentView(),
    null,
    null,
  )

internal fun com.idunnololz.summit.api.dto.piefed.LoginResponse.toLoginResponse(): LoginResponse =
  LoginResponse(
    jwt = this.jwt,
    registration_created = false,
    verify_email_sent = false,
  )

internal fun com.idunnololz.summit.api.dto.piefed.CommunityResponse.toCommunityResponse(): CommunityResponse =
  CommunityResponse(
    community_view = this.communityView.toCommunityView(),
    discussion_languages = this.discussionLanguages,
  )

internal fun com.idunnololz.summit.api.dto.piefed.GetRepliesResponse.toGetRepliesResponse(): GetRepliesResponse =
  GetRepliesResponse(
    replies = this.replies.map { it.toCommentReplyView() },
  )

internal fun com.idunnololz.summit.api.dto.piefed.GetRepliesResponse.toGetPersonMentionsResponse(): GetPersonMentionsResponse =
  GetPersonMentionsResponse(
    mentions = this.replies.map { it.toPersonMentionView() },
  )

internal fun com.idunnololz.summit.api.dto.piefed.CommentReplyView.toCommentReplyView(): CommentReplyView =
  CommentReplyView(
    comment_reply = this.commentReply.toCommentReply(),
    comment = this.comment.toComment(),
    creator = this.creator.toPerson(),
    post = this.post.toPost(),
    community = this.community.toCommunity(),
    recipient = this.recipient.toPerson(),
    counts = this.counts.toCommentAggregates(),
    creator_banned_from_community = this.creatorBannedFromCommunity,
    subscribed = this.subscribed.toSubscribedType(),
    saved = this.saved,
    creator_blocked = this.creatorBlocked,
    my_vote = this.myVote,
  )

internal fun CommentReply.toCommentReply(): com.idunnololz.summit.api.dto.lemmy.CommentReply =
  com.idunnololz.summit.api.dto.lemmy.CommentReply(
    id = this.id,
    recipient_id = this.recipientId.toLong(),
    comment_id = this.commentId,
    read = this.read,
    published = this.published,
  )

internal fun com.idunnololz.summit.api.dto.piefed.CommentReplyView.toPersonMentionView(): PersonMentionView =
  PersonMentionView(
    person_mention = this.commentReply.toPersonMention(),
    comment = this.comment.toComment(),
    creator = this.creator.toPerson(),
    post = this.post.toPost(),
    community = this.community.toCommunity(),
    recipient = this.recipient.toPerson(),
    counts = this.counts.toCommentAggregates(),
    creator_banned_from_community = this.creatorBannedFromCommunity,
    subscribed = this.subscribed.toSubscribedType(),
    saved = this.saved,
    creator_blocked = this.creatorBlocked,
    my_vote = this.myVote,
  )

internal fun CommentReply.toPersonMention(): PersonMention = PersonMention(
  id = this.id,
  recipient_id = this.recipientId.toLong(),
  comment_id = this.commentId,
  read = this.read,
  published = this.published,
)

internal fun com.idunnololz.summit.api.dto.piefed.SearchType.toSearchType(): SearchType =
  when (this) {
    com.idunnololz.summit.api.dto.piefed.SearchType.Communities -> SearchType.Communities
    com.idunnololz.summit.api.dto.piefed.SearchType.Posts -> SearchType.Posts
    com.idunnololz.summit.api.dto.piefed.SearchType.Users -> SearchType.Users
    com.idunnololz.summit.api.dto.piefed.SearchType.Url -> SearchType.Url
    com.idunnololz.summit.api.dto.piefed.SearchType.All -> SearchType.All
  }

internal fun com.idunnololz.summit.api.dto.piefed.PrivateMessageView.toPrivateMessageView(): PrivateMessageView =
  PrivateMessageView(
    this.privateMessage.toPrivateMessage(),
    this.creator.toPerson(),
    this.recipient.toPerson(),
  )

internal fun com.idunnololz.summit.api.dto.piefed.PrivateMessage.toPrivateMessage(): PrivateMessage =
  PrivateMessage(
    id = this.id,
    creator_id = this.creatorId.toLong(),
    recipient_id = this.recipientId.toLong(),
    content = this.content,
    deleted = this.deleted,
    read = this.read,
    published = this.published,
    updated = this.updated,
    ap_id = this.apId,
    local = this.local,
  )

internal fun com.idunnololz.summit.api.dto.piefed.CommentReportView.toCommentReportView(): CommentReportView =
  CommentReportView(
    comment_report = this.commentReport.toCommentReport(),
    comment = this.comment.toComment(),
    post = this.post.toPost(),
    community = this.community.toCommunity(),
    creator = this.creator.toPerson(),
    comment_creator = this.commentCreator.toPerson(),
    counts = this.counts.toCommentAggregates(),
    creator_banned_from_community = this.creatorBannedFromCommunity,
    my_vote = this.myVote,
    resolver = this.resolver?.toPerson(),
  )

internal fun com.idunnololz.summit.api.dto.piefed.CommentReport.toCommentReport(): CommentReport =
  CommentReport(
    id = this.id,
    creator_id = this.creatorId.toLong(),
    comment_id = this.commentId,
    original_comment_text = this.originalCommentText,
    reason = this.reason,
    resolved = this.resolved,
    resolver_id = this.resolverId?.toLong(),
    published = this.published,
    updated = this.updated,
  )

internal fun com.idunnololz.summit.api.dto.piefed.PostReportView.toPostReportView(): PostReportView =
  PostReportView(
    post_report = this.postReport.toPostReport(),
    post = this.post.toPost(),
    community = this.community.toCommunity(),
    creator = this.creator.toPerson(),
    post_creator = this.postCreator.toPerson(),
    creator_banned_from_community = this.creatorBannedFromCommunity,
    my_vote = this.myVote,
    counts = this.counts.toPostAggregates(),
    resolver = this.resolver?.toPerson(),
  )

internal fun com.idunnololz.summit.api.dto.piefed.PostReport.toPostReport(): PostReport =
  PostReport(
    id = this.id,
    creator_id = this.creatorId.toLong(),
    post_id = this.postId,
    original_post_name = this.originalPostName,
    original_post_url = this.originalPostUrl,
    original_post_body = this.originalPostBody,
    reason = this.reason,
    resolved = this.resolved,
    resolver_id = this.resolverId?.toLong(),
    published = this.published,
    updated = this.updated,
  )
