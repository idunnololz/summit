package com.idunnololz.summit.api.converters

import com.idunnololz.summit.api.dto.lemmy.AdminPurgeComment
import com.idunnololz.summit.api.dto.lemmy.AdminPurgeCommentView
import com.idunnololz.summit.api.dto.lemmy.AdminPurgeCommunity
import com.idunnololz.summit.api.dto.lemmy.AdminPurgeCommunityView
import com.idunnololz.summit.api.dto.lemmy.AdminPurgePerson
import com.idunnololz.summit.api.dto.lemmy.AdminPurgePersonView
import com.idunnololz.summit.api.dto.lemmy.AdminPurgePost
import com.idunnololz.summit.api.dto.lemmy.AdminPurgePostView
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
import com.idunnololz.summit.api.dto.lemmy.ListingType
import com.idunnololz.summit.api.dto.lemmy.LoginResponse
import com.idunnololz.summit.api.dto.lemmy.ModAdd
import com.idunnololz.summit.api.dto.lemmy.ModAddCommunity
import com.idunnololz.summit.api.dto.lemmy.ModAddCommunityView
import com.idunnololz.summit.api.dto.lemmy.ModAddView
import com.idunnololz.summit.api.dto.lemmy.ModBan
import com.idunnololz.summit.api.dto.lemmy.ModBanFromCommunity
import com.idunnololz.summit.api.dto.lemmy.ModBanFromCommunityView
import com.idunnololz.summit.api.dto.lemmy.ModBanView
import com.idunnololz.summit.api.dto.lemmy.ModFeaturePost
import com.idunnololz.summit.api.dto.lemmy.ModFeaturePostView
import com.idunnololz.summit.api.dto.lemmy.ModHideCommunity
import com.idunnololz.summit.api.dto.lemmy.ModHideCommunityView
import com.idunnololz.summit.api.dto.lemmy.ModLockPost
import com.idunnololz.summit.api.dto.lemmy.ModLockPostView
import com.idunnololz.summit.api.dto.lemmy.ModRemoveComment
import com.idunnololz.summit.api.dto.lemmy.ModRemoveCommentView
import com.idunnololz.summit.api.dto.lemmy.ModRemoveCommunity
import com.idunnololz.summit.api.dto.lemmy.ModRemoveCommunityView
import com.idunnololz.summit.api.dto.lemmy.ModRemovePost
import com.idunnololz.summit.api.dto.lemmy.ModRemovePostView
import com.idunnololz.summit.api.dto.lemmy.ModTransferCommunity
import com.idunnololz.summit.api.dto.lemmy.ModTransferCommunityView
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
import com.idunnololz.summit.api.dto.lemmy.SortType
import com.idunnololz.summit.api.dto.lemmy.SubscribedType
import com.idunnololz.summit.api.dto.piefed.models.Comment
import com.idunnololz.summit.api.dto.piefed.models.CommentAggregates
import com.idunnololz.summit.api.dto.piefed.models.CommentReply
import com.idunnololz.summit.api.dto.piefed.models.Community
import com.idunnololz.summit.api.dto.piefed.models.CommunityBlockView
import com.idunnololz.summit.api.dto.piefed.models.CommunityFollowerView
import com.idunnololz.summit.api.dto.piefed.models.CommunityView
import com.idunnololz.summit.api.dto.piefed.models.Instance
import com.idunnololz.summit.api.dto.piefed.models.InstanceBlockView
import com.idunnololz.summit.api.dto.piefed.models.LocalUser
import com.idunnololz.summit.api.dto.piefed.models.LocalUserView
import com.idunnololz.summit.api.dto.piefed.models.MyUserInfo
import com.idunnololz.summit.api.dto.piefed.models.Person
import com.idunnololz.summit.api.dto.piefed.models.PersonAggregates
import com.idunnololz.summit.api.dto.piefed.models.PersonBlockView
import com.idunnololz.summit.api.dto.piefed.models.Post
import com.idunnololz.summit.api.dto.piefed.models.PostAggregates
import com.idunnololz.summit.api.dto.piefed.models.SearchResponse
import com.idunnololz.summit.api.dto.piefed.models.Site
import com.idunnololz.summit.api.dto.piefed.models.UserRegistration
import com.idunnololz.summit.api.local.UserRegistrationApplication

internal fun com.idunnololz.summit.api.dto.piefed.models.PersonView.toPersonView(): PersonView =
  PersonView(
    person.toPerson(),
    counts.toPersonAggregates(),
  )

private fun Person.toPerson(): com.idunnololz.summit.api.dto.lemmy.Person =
  com.idunnololz.summit.api.dto.lemmy.Person(
    id = this.id.toLong(),
    name = this.userName,
    display_name = this.title,
    avatar = this.avatar,
    banned = this.banned,
    published = this.published,
    updated = null,
    actor_id = this.actorId,
    bio = this.about,
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

internal fun com.idunnololz.summit.api.dto.piefed.models.CommentView.toCommentView(): CommentView =
  CommentView(
    comment = this.comment.toComment(),
    creator = this.creator.toPerson(),
    post = this.post.toPost(),
    community = this.community.toCommunity(),
    counts = this.counts.toCommentAggregates(),
    creator_banned_from_community = this.creatorBannedFromCommunity,
    creator_is_moderator = this.creatorIsModerator,
    creator_is_admin = this.creatorIsAdmin,
    subscribed = this.subscribed?.toSubscribedType() ?: SubscribedType.NotSubscribed,
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
    updated = this.updated,
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

internal fun com.idunnololz.summit.api.dto.piefed.models.PostView.Subscribed.toSubscribedType() =
  when (this) {
    com.idunnololz.summit.api.dto.piefed.models.PostView.Subscribed.Subscribed -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.Subscribed
    com.idunnololz.summit.api.dto.piefed.models.PostView.Subscribed.NotSubscribed -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.NotSubscribed
    com.idunnololz.summit.api.dto.piefed.models.PostView.Subscribed.Pending -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.Pending
  }

internal fun com.idunnololz.summit.api.dto.piefed.models.CommentView.Subscribed.toSubscribedType() =
  when (this) {
    com.idunnololz.summit.api.dto.piefed.models.CommentView.Subscribed.Subscribed -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.Subscribed
    com.idunnololz.summit.api.dto.piefed.models.CommentView.Subscribed.NotSubscribed -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.NotSubscribed
    com.idunnololz.summit.api.dto.piefed.models.CommentView.Subscribed.Pending -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.Pending
  }

internal fun com.idunnololz.summit.api.dto.piefed.models.CommunityView.Subscribed.toSubscribedType() =
  when (this) {
    com.idunnololz.summit.api.dto.piefed.models.CommunityView.Subscribed.Subscribed -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.Subscribed
    com.idunnololz.summit.api.dto.piefed.models.CommunityView.Subscribed.NotSubscribed -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.NotSubscribed
    com.idunnololz.summit.api.dto.piefed.models.CommunityView.Subscribed.Pending -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.Pending
  }

internal fun com.idunnololz.summit.api.dto.piefed.models.CommentReplyView.Subscribed.toSubscribedType() =
  when (this) {
    com.idunnololz.summit.api.dto.piefed.models.CommentReplyView.Subscribed.Subscribed -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.Subscribed
    com.idunnololz.summit.api.dto.piefed.models.CommentReplyView.Subscribed.NotSubscribed -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.NotSubscribed
    com.idunnololz.summit.api.dto.piefed.models.CommentReplyView.Subscribed.Pending -> com.idunnololz.summit.api.dto.lemmy.SubscribedType.Pending
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
    updated = this.updated,
    deleted = this.deleted,
    nsfw = this.nsfw,
    embed_title = null,
    embed_description = null,
    thumbnail_url = this.thumbnailUrl,
    ap_id = this.apId,
    local = this.local,
    embed_video_url = if (this.postType == Post.PostType.Video) {
      this.url
    } else {
      null
    },
    language_id = this.languageId,
    featured_community = this.sticky,
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

internal fun com.idunnololz.summit.api.dto.piefed.models.PostView.toPostView(): PostView = PostView(
  post = this.post.toPost(),
  creator = this.creator.toPerson(),
  community = this.community.toCommunity(),
  creator_banned_from_community = this.creatorBannedFromCommunity,
  creator_is_moderator = this.creatorIsModerator,
  creator_is_admin = this.creatorIsAdmin,
  counts = this.counts.toPostAggregates(),
  subscribed = this.subscribed?.toSubscribedType() ?: SubscribedType.NotSubscribed,
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

internal fun com.idunnololz.summit.api.dto.piefed.models.CommunityModeratorView.toCommunityModeratorView() =
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
    local_user_view = this.localUserView.toLocalUserView(),
    follows = this.follows.map { it.toCommunityFollowerView() },
    moderates = this.moderates.map { it.toCommunityModeratorView() },
    community_blocks = this.communityBlocks.map { it.toCommunityBlockView() },
    person_blocks = this.personBlocks.map { it.toPersonBlockView() },
    instance_blocks = this.instanceBlocks.map { it.toInstanceBlockView() },
    discussion_languages = this.discussionLanguages.mapNotNull { it.id },
  )

internal fun CommunityFollowerView.toCommunityFollowerView(): com.idunnololz.summit.api.dto.lemmy.CommunityFollowerView =
  com.idunnololz.summit.api.dto.lemmy.CommunityFollowerView(
    community = this.community.toCommunity(),
    follower = this.follower.toPerson(),
  )

internal fun CommunityBlockView.toCommunityBlockView(): com.idunnololz.summit.api.dto.lemmy.CommunityBlockView =
  com.idunnololz.summit.api.dto.lemmy.CommunityBlockView(
    person = this.person!!.toPerson(),
    community = this.community!!.toCommunity(),
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
    site = null,
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
    local_user = this.localUser.toLocalUser().copy(
      id = this.person.id,
      person_id = this.person.id.toLong(),
    ),
    person = this.person.toPerson(),
    counts = this.counts.toPersonAggregates(),
  )

internal fun LocalUser.toLocalUser(): com.idunnololz.summit.api.dto.lemmy.LocalUser =
  com.idunnololz.summit.api.dto.lemmy.LocalUser(
    id = 0,
    person_id = 0,
    email = null,
    show_nsfw = this.showNsfw ?: false,
    theme = "",
    default_sort_type = this.defaultSortType?.toSortType(),
    default_listing_type = this.defaultListingType?.toListingType(),
    interface_language = "",
    show_avatars = false,
    send_notifications_to_email = false,
    validator_time = "",
    show_scores = this.showScores ?: true,
    show_bot_accounts = this.showBotAccounts ?: true,
    show_read_posts = this.showReadPosts ?: true,
    show_new_post_notifs = false,
    email_verified = true,
    accepted_application = true,
    totp_2fa_url = null,
  )

internal fun LocalUser.DefaultSortType.toSortType(): com.idunnololz.summit.api.dto.lemmy.SortType? =
  when (this) {
    LocalUser.DefaultSortType.Active -> SortType.Active
    LocalUser.DefaultSortType.Hot -> SortType.Hot
    LocalUser.DefaultSortType.New -> SortType.New
    LocalUser.DefaultSortType.TopHour -> SortType.TopHour
    LocalUser.DefaultSortType.TopSixHour -> SortType.TopSixHour
    LocalUser.DefaultSortType.TopTwelveHour -> SortType.TopTwelveHour
    LocalUser.DefaultSortType.TopDay -> SortType.TopDay
    LocalUser.DefaultSortType.TopWeek -> SortType.TopWeek
    LocalUser.DefaultSortType.TopMonth -> SortType.TopMonth
    LocalUser.DefaultSortType.TopThreeMonths -> SortType.TopThreeMonths
    LocalUser.DefaultSortType.TopSixMonths -> SortType.TopSixMonths
    LocalUser.DefaultSortType.TopNineMonths -> SortType.TopNineMonths
    LocalUser.DefaultSortType.TopYear -> SortType.TopYear
    LocalUser.DefaultSortType.TopAll -> SortType.TopAll
    LocalUser.DefaultSortType.Scaled -> SortType.Scaled
    LocalUser.DefaultSortType.Top -> SortType.TopAll
    LocalUser.DefaultSortType.Old -> SortType.Old
    LocalUser.DefaultSortType.Relevance -> SortType.Active
  }

internal fun LocalUser.DefaultListingType.toListingType(): com.idunnololz.summit.api.dto.lemmy.ListingType =
  when (this) {
    LocalUser.DefaultListingType.All -> ListingType.All
    LocalUser.DefaultListingType.Local -> ListingType.Local
    LocalUser.DefaultListingType.Subscribed -> ListingType.Subscribed
    LocalUser.DefaultListingType.Popular -> ListingType.All
    LocalUser.DefaultListingType.ModeratorView -> ListingType.ModeratorView
    LocalUser.DefaultListingType.Moderating -> ListingType.ModeratorView
  }

internal fun CommunityView.toCommunityView(): com.idunnololz.summit.api.dto.lemmy.CommunityView =
  com.idunnololz.summit.api.dto.lemmy.CommunityView(
    community = this.community.toCommunity(),
    subscribed = this.subscribed?.toSubscribedType() ?: SubscribedType.NotSubscribed,
    blocked = this.blocked,
    counts = this.counts.toCommunityAggregates(),
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.CommunityAggregates.toCommunityAggregates(): CommunityAggregates =
  CommunityAggregates(
    id = this.id,
    community_id = this.id,
    subscribers = this.subscriptionsCount,
    posts = this.postCount,
    comments = this.postReplyCount,
    published = this.published,
    users_active_day = this.activeDaily ?: 0,
    users_active_week = this.activeWeekly ?: 0,
    users_active_month = this.activeMonthly ?: 0,
    users_active_half_year = this.active6monthly ?: 0,
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.GetPostResponse.toPostResponse() =
  PostResponse(
    this.postView.toPostView(),
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.GetCommentResponse.toCommentResponse() =
  CommentResponse(
    this.commentView.toCommentView(),
    null,
    null,
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.UserLoginResponse.toLoginResponse(): LoginResponse =
  LoginResponse(
    jwt = this.jwt,
    registration_created = false,
    verify_email_sent = false,
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.CommunityResponse.toCommunityResponse(): CommunityResponse =
  CommunityResponse(
    community_view = this.communityView.toCommunityView(),
    discussion_languages = this.discussionLanguages,
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.UserRepliesResponse.toGetRepliesResponse(): GetRepliesResponse =
  GetRepliesResponse(
    replies = this.replies.map { it.toCommentReplyView() },
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.UserMarkAllReadResponse.toGetRepliesResponse(): GetRepliesResponse =
  GetRepliesResponse(
    replies = this.replies.map { it.toCommentReplyView() },
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.UserMentionsResponse.toGetPersonMentionsResponse(): GetPersonMentionsResponse =
  GetPersonMentionsResponse(
    mentions = this.replies.map { it.toPersonMentionView() },
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.CommentReplyView.toCommentReplyView(): CommentReplyView =
  CommentReplyView(
    comment_reply = this.commentReply.toCommentReply(),
    comment = this.comment.toComment(),
    creator = this.creator.toPerson(),
    post = this.post.toPost(),
    community = this.community.toCommunity(),
    recipient = this.recipient.toPerson(),
    counts = this.counts.toCommentAggregates(),
    creator_banned_from_community = this.creatorBannedFromCommunity,
    subscribed = this.subscribed?.toSubscribedType() ?: SubscribedType.NotSubscribed,
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

internal fun com.idunnololz.summit.api.dto.piefed.models.CommentReplyView.toPersonMentionView(): PersonMentionView =
  PersonMentionView(
    person_mention = this.commentReply.toPersonMention(),
    comment = this.comment.toComment(),
    creator = this.creator.toPerson(),
    post = this.post.toPost(),
    community = this.community.toCommunity(),
    recipient = this.recipient.toPerson(),
    counts = this.counts.toCommentAggregates(),
    creator_banned_from_community = this.creatorBannedFromCommunity,
    subscribed = this.subscribed?.toSubscribedType() ?: SubscribedType.NotSubscribed,
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

internal fun com.idunnololz.summit.api.dto.piefed.models.SearchResponse.Type.toSearchType(): SearchType =
  when (this) {
    SearchResponse.Type.Communities -> SearchType.Communities
    SearchResponse.Type.Posts -> SearchType.Posts
    SearchResponse.Type.Users -> SearchType.Users
    SearchResponse.Type.Url -> SearchType.Url
    SearchResponse.Type.Comments -> SearchType.Comments
  }

internal fun com.idunnololz.summit.api.dto.piefed.models.PrivateMessageView.toPrivateMessageView(): PrivateMessageView =
  PrivateMessageView(
    this.privateMessage.toPrivateMessage(),
    this.creator.toPerson(),
    this.recipient.toPerson(),
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.PrivateMessage.toPrivateMessage(): PrivateMessage =
  PrivateMessage(
    id = this.id,
    creator_id = this.creatorId.toLong(),
    recipient_id = this.recipientId.toLong(),
    content = this.content,
    deleted = this.deleted,
    read = this.read,
    published = this.published,
    updated = null,
    ap_id = this.apId,
    local = this.local,
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.CommentReportView.toCommentReportView(): CommentReportView =
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
    resolver = null,
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.CommentReport.toCommentReport(): CommentReport =
  CommentReport(
    id = this.id,
    creator_id = this.creatorId.toLong(),
    comment_id = this.commentId,
    original_comment_text = this.originalCommentText,
    reason = this.reason,
    resolved = this.resolved,
    resolver_id = null,
    published = this.published,
    updated = this.updated,
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.PostReportView.toPostReportView(): PostReportView =
  PostReportView(
    post_report = this.postReport.toPostReport(),
    post = this.post.toPost(),
    community = this.community.toCommunity(),
    creator = this.creator.toPerson(),
    post_creator = this.postCreator.toPerson(),
    creator_banned_from_community = this.creatorBannedFromCommunity,
    my_vote = null,
    counts = this.counts.toPostAggregates(),
    resolver = null,
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.PostReport.toPostReport(): PostReport =
  PostReport(
    id = this.id,
    creator_id = this.creatorId.toLong(),
    post_id = this.postId,
    original_post_name = this.originalPostName,
    original_post_url = null,
    original_post_body = this.originalPostBody,
    reason = this.reason,
    resolved = this.resolved,
    resolver_id = null,
    published = this.published!!,
    updated = null,
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.ModRemovePostView.toModRemovePostView(): ModRemovePostView? {
  return ModRemovePostView(
    mod_remove_post = modRemovePost.toModRemovePost() ?: return null,
    moderator = moderator?.toPerson(),
    post = post?.toPost() ?: return null,
    community = community?.toCommunity() ?: return null,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModRemovePost.toModRemovePost(): ModRemovePost? {
  return ModRemovePost(
    id = id,
    mod_person_id = modPersonId?.toLong() ?: return null,
    post_id = postId ?: return null,
    reason = reason,
    removed = removed,
    when_ = `when`,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModLockPostView.toModLockPostView(): ModLockPostView? {
  return ModLockPostView(
    mod_lock_post = modLockPost.toModLockPost() ?: return null,
    moderator = moderator?.toPerson(),
    post = post?.toPost() ?: return null,
    community = community?.toCommunity() ?: return null,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModLockPost.toModLockPost(): ModLockPost? {
  return ModLockPost(
    id = id,
    mod_person_id = modPersonId?.toLong() ?: return null,
    post_id = postId ?: return null,
    locked = locked,
    when_ = `when`,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModFeaturePostView.toModFeaturePostView(): ModFeaturePostView? {
  return ModFeaturePostView(
    mod_feature_post = modFeaturePost.toModFeaturePost() ?: return null,
    moderator = moderator?.toPerson(),
    post = post?.toPost() ?: return null,
    community = community?.toCommunity() ?: return null,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModFeaturePost.toModFeaturePost(): ModFeaturePost? {
  return ModFeaturePost(
    id = id,
    mod_person_id = modPersonId?.toLong() ?: return null,
    post_id = postId ?: return null,
    featured = featured,
    when_ = `when`,
    is_featured_community = isFeaturedCommunity,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModRemoveCommentView.toModRemoveCommentView(): ModRemoveCommentView? {
  return ModRemoveCommentView(
    mod_remove_comment = modRemoveComment.toModRemoveComment() ?: return null,
    moderator = moderator?.toPerson(),
    comment = comment?.toComment() ?: return null,
    commenter = commenter?.toPerson() ?: return null,
    post = post?.toPost() ?: return null,
    community = community?.toCommunity() ?: return null,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModRemoveComment.toModRemoveComment(): ModRemoveComment? {
  return ModRemoveComment(
    id = id,
    mod_person_id = modPersonId?.toLong() ?: return null,
    comment_id = commentId ?: return null,
    reason = reason,
    removed = removed,
    when_ = `when`,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModRemoveCommunityView.toModRemoveCommunityView(): ModRemoveCommunityView? {
  return ModRemoveCommunityView(
    mod_remove_community = modRemoveCommunity.toModRemoveCommunity() ?: return null,
    moderator = moderator?.toPerson(),
    community = community?.toCommunity() ?: return null,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModRemoveCommunity.toModRemoveCommunity(): ModRemoveCommunity? {
  return ModRemoveCommunity(
    id = id,
    mod_person_id = modPersonId?.toLong() ?: return null,
    community_id = communityId ?: return null,
    reason = reason,
    removed = removed,
    expires = null,
    when_ = `when`,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModBanFromCommunityView.toModBanFromCommunityView(): ModBanFromCommunityView? {
  return ModBanFromCommunityView(
    mod_ban_from_community = modBanFromCommunity.toModBanFromCommunity() ?: return null,
    moderator = moderator?.toPerson(),
    community = community?.toCommunity() ?: return null,
    banned_person = bannedPerson?.toPerson() ?: return null,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModBanFromCommunity.toModBanFromCommunity(): ModBanFromCommunity? {
  return ModBanFromCommunity(
    id = id,
    mod_person_id = modPersonId?.toLong() ?: return null,
    other_person_id = otherPersonId?.toLong() ?: return null,
    community_id = communityId ?: return null,
    reason = reason,
    banned = banned,
    expires = expires,
    when_ = `when`,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModBanView.toModBanView(): ModBanView? {
  return ModBanView(
    mod_ban = modBan.toModBan() ?: return null,
    moderator = moderator?.toPerson(),
    banned_person = bannedPerson?.toPerson() ?: return null,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModBan.toModBan(): ModBan? {
  return ModBan(
    id = id,
    mod_person_id = modPersonId?.toLong() ?: return null,
    other_person_id = otherPersonId?.toLong() ?: return null,
    reason = reason,
    banned = banned,
    expires = expires,
    when_ = `when`,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModAddCommunityView.toModAddCommunityView(): ModAddCommunityView? {
  return ModAddCommunityView(
    mod_add_community = modAddCommunity.toModAddCommunity() ?: return null,
    moderator = moderator?.toPerson(),
    community = community?.toCommunity() ?: return null,
    modded_person = moddedPerson?.toPerson() ?: return null,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModAddCommunity.toModAddCommunity(): ModAddCommunity? {
  return ModAddCommunity(
    id = id,
    mod_person_id = modPersonId?.toLong() ?: return null,
    other_person_id = otherPersonId?.toLong() ?: return null,
    community_id = communityId ?: return null,
    removed = removed,
    when_ = `when`,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModTransferCommunityView.toModTransferCommunityView(): ModTransferCommunityView? {
  return ModTransferCommunityView(
    mod_transfer_community = modTransferCommunity.toModTransferCommunity() ?: return null,
    moderator = moderator?.toPerson(),
    community = community.toCommunity(),
    modded_person = moddedPerson?.toPerson() ?: return null,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModTransferCommunity.toModTransferCommunity(): ModTransferCommunity? {
  return ModTransferCommunity(
    id = id,
    mod_person_id = modPersonId?.toLong() ?: return null,
    other_person_id = otherPersonId?.toLong() ?: return null,
    community_id = communityId ?: return null,
    when_ = `when`,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModAddView.toModAddView(): ModAddView? {
  return ModAddView(
    mod_add = modAdd.toModAdd() ?: return null,
    moderator = moderator?.toPerson(),
    modded_person = moddedPerson?.toPerson() ?: return null,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModAdd.toModAdd(): ModAdd? {
  return ModAdd(
    id = id,
    mod_person_id = modPersonId?.toLong() ?: return null,
    other_person_id = otherPersonId?.toLong() ?: return null,
    removed = removed,
    when_ = `when`,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.AdminPurgePersonView.toAdminPurgePersonView(): AdminPurgePersonView? {
  return AdminPurgePersonView(
    admin_purge_person = adminPurgePerson.toAdminPurgePerson() ?: return null,
    admin = admin?.toPerson(),
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.AdminPurgePerson.toAdminPurgePerson(): AdminPurgePerson? =
  AdminPurgePerson(
    id = id,
    admin_person_id = adminPersonId.toLong(),
    reason = reason,
    when_ = `when`,
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.AdminPurgeCommunityView.toAdminPurgeCommunityView(): AdminPurgeCommunityView =
  AdminPurgeCommunityView(
    admin_purge_community = adminPurgeCommunity.toAdminPurgeCommunity(),
    admin = admin?.toPerson(),
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.AdminPurgeCommunity.toAdminPurgeCommunity() =
  AdminPurgeCommunity(
    id = id,
    admin_person_id = adminPersonId.toLong(),
    reason = reason,
    when_ = `when`,
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.AdminPurgePostView.toAdminPurgePostView(): AdminPurgePostView? {
  return AdminPurgePostView(
    admin_purge_post = adminPurgePost.toAdminPurgePost() ?: return null,
    community = community.toCommunity(),
    admin = admin?.toPerson(),
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.AdminPurgePost.toAdminPurgePost(): AdminPurgePost? {
  return AdminPurgePost(
    id = id,
    admin_person_id = adminPersonId.toLong(),
    community_id = communityId ?: return null,
    reason = reason,
    when_ = `when`,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.AdminPurgeCommentView.toAdminPurgeCommentView(): AdminPurgeCommentView? {
  return AdminPurgeCommentView(
    admin_purge_comment = adminPurgeComment.toAdminPurgeComment() ?: return null,
    post = post.toPost(),
    admin = admin?.toPerson(),
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.AdminPurgeComment.toAdminPurgeComment(): AdminPurgeComment? =
  AdminPurgeComment(
    id = id,
    admin_person_id = adminPersonId.toLong(),
    post_id = postId,
    reason = reason,
    when_ = `when`,
  )

internal fun com.idunnololz.summit.api.dto.piefed.models.ModHideCommunityView.toModHideCommunityView(): ModHideCommunityView? {
  return ModHideCommunityView(
    mod_hide_community = modHideCommunity.toModHideCommunity() ?: return null,
    community = community.toCommunity(),
    admin = admin?.toPerson(),
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.ModHideCommunity.toModHideCommunity(): ModHideCommunity? {
  return ModHideCommunity(
    id = id,
    community_id = communityId ?: return null,
    mod_person_id = modPersonId?.toLong() ?: return null,
    hidden = hidden,
    when_ = `when`,
    reason = reason,
  )
}

internal fun com.idunnololz.summit.api.dto.piefed.models.UserRegistration.toUserRegistration(
  instance: String,
) = UserRegistrationApplication(
  id = userId,
  answer = answer,
  email = email,
  ipAddress = ipAddress,
  userId = userId.toLong(),
  userName = userName,
  appliedAt = appliedAt,
  countryCode = countryCode,
  throwawayEmail = throwawayEmail,
  status = status?.toStatus() ?: UserRegistrationApplication.Status.NoDecision,
  approvedBy = approvedBy?.toPerson(),
  approvedAt = approvedAt,
  referrer = referrer,
  instance = instance,
  isRead = false,
)

internal fun UserRegistration.Status.toStatus() = when (this) {
  UserRegistration.Status.approved -> UserRegistrationApplication.Status.Approved
  UserRegistration.Status.awaiting_review -> UserRegistrationApplication.Status.NoDecision
}

// internal fun com.idunnololz.summit.api.dto.piefed.models.ModRemovePostView.toModRemovePostView(): ModRemovePostView =
//  ModRemovePostView(
//    mod_remove_post = modRemovePost,
//    moderator,
//    post = post?.toPost(),
//    community,
//  )
