package com.idunnololz.summit.api

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.idunnololz.summit.api.converters.getGetPostsResponse
import com.idunnololz.summit.api.converters.toCommentReplyView
import com.idunnololz.summit.api.converters.toCommentReportView
import com.idunnololz.summit.api.converters.toCommentSortType
import com.idunnololz.summit.api.converters.toCommentView
import com.idunnololz.summit.api.converters.toCommunityModeratorView
import com.idunnololz.summit.api.converters.toCommunitySortType
import com.idunnololz.summit.api.converters.toCommunityView
import com.idunnololz.summit.api.converters.toGetSiteResponse
import com.idunnololz.summit.api.converters.toInstance
import com.idunnololz.summit.api.converters.toModlogKindFilter
import com.idunnololz.summit.api.converters.toPerson
import com.idunnololz.summit.api.converters.toPersonMentionView
import com.idunnololz.summit.api.converters.toPersonView
import com.idunnololz.summit.api.converters.toPostReportView
import com.idunnololz.summit.api.converters.toPostSortType
import com.idunnololz.summit.api.converters.toPostView
import com.idunnololz.summit.api.converters.toPrivateMessageReportView
import com.idunnololz.summit.api.converters.toPrivateMessageView
import com.idunnololz.summit.api.converters.toResolveObjectResponse
import com.idunnololz.summit.api.converters.toSearchType
import com.idunnololz.summit.api.converters.toSite
import com.idunnololz.summit.api.converters.toSiteMetadata
import com.idunnololz.summit.api.converters.toTimeInSeconds
import com.idunnololz.summit.api.converters.toType
import com.idunnololz.summit.api.converters.toUserRegistrationApplication
import com.idunnololz.summit.api.converters.toVoteView
import com.idunnololz.summit.api.dto.lemmy.AddModToCommunity
import com.idunnololz.summit.api.dto.lemmy.AddModToCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.ApproveRegistrationApplication
import com.idunnololz.summit.api.dto.lemmy.BanFromCommunity
import com.idunnololz.summit.api.dto.lemmy.BanFromCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.BanPerson
import com.idunnololz.summit.api.dto.lemmy.BanPersonResponse
import com.idunnololz.summit.api.dto.lemmy.BlockCommunity
import com.idunnololz.summit.api.dto.lemmy.BlockCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.BlockInstance
import com.idunnololz.summit.api.dto.lemmy.BlockInstanceResponse
import com.idunnololz.summit.api.dto.lemmy.BlockPerson
import com.idunnololz.summit.api.dto.lemmy.BlockPersonResponse
import com.idunnololz.summit.api.dto.lemmy.CaptchaResponse
import com.idunnololz.summit.api.dto.lemmy.ChangePassword
import com.idunnololz.summit.api.dto.lemmy.CommentReportResponse
import com.idunnololz.summit.api.dto.lemmy.CommentResponse
import com.idunnololz.summit.api.dto.lemmy.CommunityResponse
import com.idunnololz.summit.api.dto.lemmy.CreateComment
import com.idunnololz.summit.api.dto.lemmy.CreateCommentLike
import com.idunnololz.summit.api.dto.lemmy.CreateCommentReport
import com.idunnololz.summit.api.dto.lemmy.CreateCommunity
import com.idunnololz.summit.api.dto.lemmy.CreatePost
import com.idunnololz.summit.api.dto.lemmy.CreatePostLike
import com.idunnololz.summit.api.dto.lemmy.CreatePostReport
import com.idunnololz.summit.api.dto.lemmy.CreatePrivateMessage
import com.idunnololz.summit.api.dto.lemmy.CreatePrivateMessageReport
import com.idunnololz.summit.api.dto.lemmy.DeleteComment
import com.idunnololz.summit.api.dto.lemmy.DeleteCommunity
import com.idunnololz.summit.api.dto.lemmy.DeleteImage
import com.idunnololz.summit.api.dto.lemmy.DeletePost
import com.idunnololz.summit.api.dto.lemmy.DistinguishComment
import com.idunnololz.summit.api.dto.lemmy.EditComment
import com.idunnololz.summit.api.dto.lemmy.EditCommunity
import com.idunnololz.summit.api.dto.lemmy.EditPost
import com.idunnololz.summit.api.dto.lemmy.FeaturePost
import com.idunnololz.summit.api.dto.lemmy.FederatedInstances
import com.idunnololz.summit.api.dto.lemmy.FollowCommunity
import com.idunnololz.summit.api.dto.lemmy.GetCaptchaResponse
import com.idunnololz.summit.api.dto.lemmy.GetComments
import com.idunnololz.summit.api.dto.lemmy.GetCommentsResponse
import com.idunnololz.summit.api.dto.lemmy.GetCommunity
import com.idunnololz.summit.api.dto.lemmy.GetCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.GetModlog
import com.idunnololz.summit.api.dto.lemmy.GetPersonDetails
import com.idunnololz.summit.api.dto.lemmy.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.lemmy.GetPersonMentions
import com.idunnololz.summit.api.dto.lemmy.GetPersonMentionsResponse
import com.idunnololz.summit.api.dto.lemmy.GetPost
import com.idunnololz.summit.api.dto.lemmy.GetPostResponse
import com.idunnololz.summit.api.dto.lemmy.GetPosts
import com.idunnololz.summit.api.dto.lemmy.GetPostsResponse
import com.idunnololz.summit.api.dto.lemmy.GetPrivateMessages
import com.idunnololz.summit.api.dto.lemmy.GetReplies
import com.idunnololz.summit.api.dto.lemmy.GetRepliesResponse
import com.idunnololz.summit.api.dto.lemmy.GetSite
import com.idunnololz.summit.api.dto.lemmy.GetSiteMetadata
import com.idunnololz.summit.api.dto.lemmy.GetSiteMetadataResponse
import com.idunnololz.summit.api.dto.lemmy.GetSiteResponse
import com.idunnololz.summit.api.dto.lemmy.GetUnreadCount
import com.idunnololz.summit.api.dto.lemmy.GetUnreadRegistrationApplicationCount
import com.idunnololz.summit.api.dto.lemmy.GetUnreadRegistrationApplicationCountResponse
import com.idunnololz.summit.api.dto.lemmy.HideCommunity
import com.idunnololz.summit.api.dto.lemmy.Instance
import com.idunnololz.summit.api.dto.lemmy.ListCommentLikes
import com.idunnololz.summit.api.dto.lemmy.ListCommentLikesResponse
import com.idunnololz.summit.api.dto.lemmy.ListCommentReports
import com.idunnololz.summit.api.dto.lemmy.ListCommentReportsResponse
import com.idunnololz.summit.api.dto.lemmy.ListCommunities
import com.idunnololz.summit.api.dto.lemmy.ListCommunitiesResponse
import com.idunnololz.summit.api.dto.lemmy.ListMedia
import com.idunnololz.summit.api.dto.lemmy.ListMediaResponse
import com.idunnololz.summit.api.dto.lemmy.ListPostLikes
import com.idunnololz.summit.api.dto.lemmy.ListPostLikesResponse
import com.idunnololz.summit.api.dto.lemmy.ListPostReports
import com.idunnololz.summit.api.dto.lemmy.ListPostReportsResponse
import com.idunnololz.summit.api.dto.lemmy.ListPrivateMessageReports
import com.idunnololz.summit.api.dto.lemmy.ListPrivateMessageReportsResponse
import com.idunnololz.summit.api.dto.lemmy.ListRegistrationApplications
import com.idunnololz.summit.api.dto.lemmy.LocalImage
import com.idunnololz.summit.api.dto.lemmy.LocalImageView
import com.idunnololz.summit.api.dto.lemmy.LockPost
import com.idunnololz.summit.api.dto.lemmy.Login
import com.idunnololz.summit.api.dto.lemmy.LoginResponse
import com.idunnololz.summit.api.dto.lemmy.MarkAllAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkCommentReplyAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkPersonMentionAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkPostAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkPrivateMessageAsRead
import com.idunnololz.summit.api.dto.lemmy.ModlogActionType
import com.idunnololz.summit.api.dto.lemmy.PersonMentionResponse
import com.idunnololz.summit.api.dto.lemmy.PostFeatureType
import com.idunnololz.summit.api.dto.lemmy.PostReportResponse
import com.idunnololz.summit.api.dto.lemmy.PostResponse
import com.idunnololz.summit.api.dto.lemmy.PrivateMessageReportResponse
import com.idunnololz.summit.api.dto.lemmy.PrivateMessageResponse
import com.idunnololz.summit.api.dto.lemmy.PrivateMessagesResponse
import com.idunnololz.summit.api.dto.lemmy.PurgeComment
import com.idunnololz.summit.api.dto.lemmy.PurgeCommunity
import com.idunnololz.summit.api.dto.lemmy.PurgePerson
import com.idunnololz.summit.api.dto.lemmy.PurgePost
import com.idunnololz.summit.api.dto.lemmy.Register
import com.idunnololz.summit.api.dto.lemmy.RemoveComment
import com.idunnololz.summit.api.dto.lemmy.RemoveCommunity
import com.idunnololz.summit.api.dto.lemmy.RemovePost
import com.idunnololz.summit.api.dto.lemmy.ResolveCommentReport
import com.idunnololz.summit.api.dto.lemmy.ResolveObject
import com.idunnololz.summit.api.dto.lemmy.ResolveObjectResponse
import com.idunnololz.summit.api.dto.lemmy.ResolvePostReport
import com.idunnololz.summit.api.dto.lemmy.ResolvePrivateMessageReport
import com.idunnololz.summit.api.dto.lemmy.SaveComment
import com.idunnololz.summit.api.dto.lemmy.SavePost
import com.idunnololz.summit.api.dto.lemmy.SaveUserSettings
import com.idunnololz.summit.api.dto.lemmy.Search
import com.idunnololz.summit.api.dto.lemmy.SearchResponse
import com.idunnololz.summit.api.dto.lemmy.SuccessResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.DeleteImageParamsI
import com.idunnololz.summit.api.dto.lemmy.v4.models.GetCommentsI
import com.idunnololz.summit.api.dto.lemmy.v4.models.GetModlogI
import com.idunnololz.summit.api.dto.lemmy.v4.models.GetPostsI
import com.idunnololz.summit.api.dto.lemmy.v4.models.GetSiteMetadataI
import com.idunnololz.summit.api.dto.lemmy.v4.models.ListCommunitiesI
import com.idunnololz.summit.api.dto.lemmy.v4.models.ListMediaI
import com.idunnololz.summit.api.dto.lemmy.v4.models.ListRegistrationApplicationsI
import com.idunnololz.summit.api.dto.lemmy.v4.models.ListReportsI
import com.idunnololz.summit.api.dto.lemmy.v4.models.MarkNotificationAsRead
import com.idunnololz.summit.api.dto.lemmy.v4.models.ModlogKindFilter
import com.idunnololz.summit.api.dto.lemmy.v4.models.NotificationTypeFilter
import com.idunnololz.summit.api.dto.lemmy.v4.models.ReportType
import com.idunnololz.summit.api.dto.lemmy.v4.models.ResolveObjectI
import com.idunnololz.summit.api.dto.lemmy.v4.models.ResolveObjectView
import com.idunnololz.summit.api.dto.lemmy.v4.models.SearchI
import com.idunnololz.summit.api.dto.lemmy.v4.models.UserBlockInstanceCommunitiesParams
import com.idunnololz.summit.api.dto.other.ListInboxArgs
import com.idunnololz.summit.api.local.GetModlogResponse
import com.idunnololz.summit.api.local.PagedResponseRegistrationApplicationView
import com.idunnololz.summit.api.local.UnreadCount
import com.idunnololz.summit.api.local.UserRegistrationApplication
import com.idunnololz.summit.api.local.toModEvent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

class LemmyApiV4Adapter(
  private val api: LemmyApiV4,
  override val instance: String,
  private val apiInfo: ApiInfo,
) : LemmyLikeApi {

  companion object {
    private val gson = GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create()
  }

  override fun supportsFeature(apiFeature: ApiFeature): Boolean =
    apiInfo.supportsFeature(apiFeature)

  override suspend fun getSite(
    authorization: String?,
    args: GetSite,
    force: Boolean,
  ): Result<GetSiteResponse> = retrofitErrorHandler {
    api.getSite(
      generateHeaders(authorization, force),
      args.serializeToMap(),
    )
  }.map {
    it.toGetSiteResponse()
  }

  override suspend fun getPosts(
    authorization: String?,
    args: GetPosts,
    force: Boolean,
  ): Result<GetPostsResponse> = retrofitErrorHandler {
    api.getPosts(
      generateHeaders(authorization, force),
      GetPostsI(
        limit = args.limit,
        pageCursor = args.page_cursor,
        searchUrlOnly = null,
        searchTitleOnly = null,
        searchTerm = null,
        noCommentsOnly = null,
        markAsRead = null,
        showNsfw = null,
        showRead = args.show_read,
        showHidden = null,
        multiCommunityName = null,
        multiCommunityId = null,
        creatorUsername = null,
        creatorId = null,
        communityName = null,
        communityId = null,
        timeRangeSeconds = args.sort?.toTimeInSeconds()?.toInt(),
        sort = args.sort?.toPostSortType(),
        type = args.type_?.toType(),
      ).serializeToMap(),
    )
  }.map {
    it.getGetPostsResponse()
  }

  override suspend fun getPost(
    authorization: String?,
    args: GetPost,
    force: Boolean,
  ): Result<GetPostResponse> = retrofitErrorHandler {
    api.getPost(
      generateHeaders(authorization, force),
      args.serializeToMap(),
    )
  }.map {
    GetPostResponse(
      post_view = it.postView.toPostView(),
      community_view = it.communityView.toCommunityView(),
      moderators = it.moderators.map { it.toCommunityModeratorView() },
      cross_posts = it.crossPosts.map { it.toPostView() },
      online = 0,
    )
  }

  override suspend fun login(args: Login): Result<LoginResponse> = retrofitErrorHandler {
    api.login(
      generateHeaders(null, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.Login(
        password = args.password,
        usernameOrEmail = args.username_or_email,
        totp2faToken = args.totp_2fa_token,
      ),
    )
  }.map {
    LoginResponse(
      jwt = it.jwt,
      registration_created = it.registrationCreated,
      verify_email_sent = it.verifyEmailSent,
    )
  }

  override suspend fun likePost(
    authorization: String?,
    args: CreatePostLike,
  ): Result<PostResponse> = retrofitErrorHandler {
    api.likePost(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.CreatePostLike(
        postId = args.post_id,
        isUpvote = if (args.score > 0) {
          true
        } else if (args.score < 0) {
          false
        } else {
          null
        }
      )
    )
  }.map {
    PostResponse(
      post_view = it.postView.toPostView(),
    )
  }

  override suspend fun likeComment(
    authorization: String?,
    args: CreateCommentLike,
  ): Result<CommentResponse> = retrofitErrorHandler {
    api.likeComment(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.CreateCommentLike(
        commentId = args.comment_id,
        isUpvote = if (args.score > 0) {
          true
        } else if (args.score < 0) {
          false
        } else {
          null
        }
      )
    )
  }.map {
    CommentResponse(
      comment_view = it.commentView.toCommentView(),
      recipient_ids = null,
      form_id = null,
    )
  }

  override suspend fun listCommentVotes(
    authorization: String?,
    args: ListCommentLikes,
    force: Boolean,
  ): Result<ListCommentLikesResponse> = retrofitErrorHandler {
    api.listCommentVotes(
      generateHeaders(authorization, force),
      args.serializeToMap()
    )
  }.map {
    ListCommentLikesResponse(
      it.items.map { it.toVoteView() },
      next_page = it.nextPage,
      prev_page = it.prevPage,
    )
  }

  override suspend fun listPostVotes(
    authorization: String?,
    args: ListPostLikes,
    force: Boolean,
  ): Result<ListPostLikesResponse> = retrofitErrorHandler {
    api.listPostVotes(
      generateHeaders(authorization, force),
      args.serializeToMap()
    )
  }.map {
    ListPostLikesResponse(
      it.items.map { it.toVoteView() },
      next_page = it.nextPage,
      prev_page = it.prevPage,
    )
  }

  override suspend fun createComment(
    authorization: String?,
    args: CreateComment,
  ): Result<CommentResponse> = retrofitErrorHandler {
    api.createComment(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.CreateComment(
        postId = args.post_id,
        content = args.content,
        languageId = args.language_id,
        parentId = args.parent_id,
      )
    )
  }.map {
    CommentResponse(
      it.commentView.toCommentView(),
      null,
      null,
    )
  }

  override suspend fun editComment(
    authorization: String?,
    args: EditComment,
  ): Result<CommentResponse> = retrofitErrorHandler {
    api.editComment(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.EditComment(
        commentId = args.comment_id,
        languageId = args.language_id,
        content = args.content,
      )
    )
  }.map {
    CommentResponse(
      it.commentView.toCommentView(),
      null,
      null,
    )
  }

  override suspend fun deleteComment(
    authorization: String?,
    args: DeleteComment,
  ): Result<CommentResponse> = retrofitErrorHandler {
    api.deleteComment(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.DeleteComment(
        deleted = args.deleted,
        commentId = args.comment_id,
      )
    )
  }.map {
    CommentResponse(
      it.commentView.toCommentView(),
      null,
      null,
    )
  }

  override suspend fun savePost(
    authorization: String?,
    args: SavePost,
  ): Result<PostResponse> = retrofitErrorHandler {
    api.savePost(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.SavePost(
        args.save,
        args.post_id,
      )
    )
  }.map {
    PostResponse(
      it.postView.toPostView(),
    )
  }

  override suspend fun markPostAsRead(
    authorization: String?,
    args: MarkPostAsRead,
  ): Result<SuccessResponse> = retrofitErrorHandler {
    api.markPostAsRead(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.MarkPostAsRead(
        args.read,
        args.post_id,
      )
    )
  }.map {
    SuccessResponse(
      it.success,
    )
  }

  override suspend fun saveComment(
    authorization: String?,
    args: SaveComment,
  ): Result<CommentResponse> = retrofitErrorHandler {
    api.saveComment(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.SaveComment(
        args.save,
        args.comment_id,
      )
    )
  }.map {
    CommentResponse(
      it.commentView.toCommentView(),
      null,
      null,
    )
  }


  override suspend fun getComments(
    authorization: String?,
    args: GetComments,
    force: Boolean,
  ): Result<GetCommentsResponse> = retrofitErrorHandler {
    api.getComments(
      generateHeaders(authorization, force),
      GetCommentsI(
        searchTerm = null,
        parentId = args.parent_id,
        postId = args.post_id,
        creatorUsername = null,
        creatorId = null,
        communityName = args.community_name,
        communityId = args.community_id,
        limit = args.limit,
        pageCursor = args.page_cursor,
        maxDepth = args.max_depth,
        timeRangeSeconds = null,
        sort = args.sort?.toCommentSortType(),
        type = args.type_?.toType(),
      ).serializeToMap(),
    )
  }.map {
    GetCommentsResponse(
      it.items.map { it.toCommentView() },
      next_page = it.nextPage,
      prev_page = it.prevPage,
    )
  }

  override suspend fun distinguishComment(
    authorization: String?,
    args: DistinguishComment,
  ): Result<CommentResponse> = retrofitErrorHandler {
    api.distinguishComment(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.DistinguishComment(
        distinguished = args.distinguished,
        commentId = args.comment_id,
      ),
    )
  }.map {
    CommentResponse(
      it.commentView.toCommentView(),
      null,
      null,
    )
  }

  override suspend fun removeComment(
    authorization: String?,
    args: RemoveComment,
  ): Result<CommentResponse> = retrofitErrorHandler {
    api.removeComment(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.RemoveComment(
        args.reason ?: "",
        args.removed,
        args.comment_id,
      ),
    )
  }.map {
    CommentResponse(
      it.commentView.toCommentView(),
      null,
      null,
    )
  }

  override suspend fun getCommunity(
    authorization: String?,
    args: GetCommunity,
    force: Boolean,
  ): Result<GetCommunityResponse> = retrofitErrorHandler {
    api.getCommunity(
      generateHeaders(authorization, force),
      args.serializeToMap(),
    )
  }.map {
    GetCommunityResponse(
      it.communityView.toCommunityView(),
      it.site?.toSite(),
      it.moderators.map { it.toCommunityModeratorView() },
      null,
      it.discussionLanguages.map { it.toInt() },
      null,
    )
  }

  override suspend fun createCommunity(
    authorization: String?,
    args: CreateCommunity,
  ): Result<CommunityResponse> = retrofitErrorHandler {
    api.createCommunity(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.CreateCommunity(
        name = args.name,
        visibility = null,
        discussionLanguages = args.discussion_languages?.map { it.toDouble() },
        postingRestrictedToMods = args.posting_restricted_to_mods,
        nsfw = args.nsfw,
        summary = args.description,
        sidebar = null,
        title = args.title,
      ),
    )
  }.map {
    CommunityResponse(
      it.communityView.toCommunityView(),
      it.discussionLanguages.map { it },
    )
  }

  override suspend fun updateCommunity(
    authorization: String?,
    args: EditCommunity,
  ): Result<CommunityResponse> = retrofitErrorHandler {
    api.updateCommunity(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.EditCommunity(
        args.community_id,
        null,
        args.discussion_languages?.map { it.toDouble() },
        args.posting_restricted_to_mods,
        args.nsfw,
        args.description,
        null,
        args.title,
      ),
    )
  }.map {
    CommunityResponse(
      it.communityView.toCommunityView(),
      it.discussionLanguages.map { it },
    )
  }

  override suspend fun deleteCommunity(
    authorization: String?,
    args: DeleteCommunity,
  ): Result<CommunityResponse> = retrofitErrorHandler {
    api.deleteCommunity(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.DeleteCommunity(
        args.deleted,
        args.community_id,
      ),
    )
  }.map {
    CommunityResponse(
      it.communityView.toCommunityView(),
      it.discussionLanguages.map { it },
    )
  }

  override suspend fun getCommunityList(
    authorization: String?,
    args: ListCommunities,
    force: Boolean,
  ): Result<ListCommunitiesResponse> = retrofitErrorHandler {
    api.getCommunityList(
      generateHeaders(authorization, force),
      ListCommunitiesI(
        limit = args.limit,
        pageCursor = args.page_cursor,
        searchTitleOnly = null,
        searchTerm = null,
        multiCommunityId = null,
        showNsfw = null,
        timeRangeSeconds = args.sort?.toTimeInSeconds(),
        sort = args.sort?.toCommunitySortType(),
        type = args.type_?.toType(),
      ).serializeToMap(),
    )
  }.map {
    ListCommunitiesResponse(
      it.items.map { it.toCommunityView() },
      next_page = it.nextPage,
      prev_page = it.prevPage,
    )
  }

  override suspend fun getPersonDetails(
    authorization: String?,
    args: GetPersonDetails,
    force: Boolean,
  ): Result<GetPersonDetailsResponse> = retrofitErrorHandler {
    api.getPersonDetails(
      generateHeaders(authorization, force),
      args.serializeToMap(),
    )
  }.map {
    GetPersonDetailsResponse(
      it.personView.toPersonView(),
      listOf(),
      listOf(),
      it.moderates.map { it.toCommunityModeratorView() },
    )
  }

  override suspend fun changePassword(
    authorization: String?,
    args: ChangePassword,
  ): Result<LoginResponse> = retrofitErrorHandler {
    api.changePassword(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.ChangePassword(
        oldPassword = args.old_password,
        newPassword = args.new_password,
        newPasswordVerify = args.new_password_verify,
      ),
    )
  }.map {
    LoginResponse(
      it.jwt,
      it.registrationCreated,
      it.verifyEmailSent,
    )
  }

  override suspend fun getReplies(
    authorization: String?,
    args: GetReplies,
    force: Boolean,
  ): Result<GetRepliesResponse> = retrofitErrorHandler {
    api.listInbox(
      generateHeaders(authorization, false),
      ListInboxArgs(
        limit = args.limit,
        page_cursor = args.page_cursor,
        creator_id = null,
        unread_only = args.unread_only,
        type = NotificationTypeFilter.reply,
      ).serializeToMap()
    )
  }.map {
    GetRepliesResponse(
      replies = it.items.map { it.toCommentReplyView() }
    )
  }

  override suspend fun markCommentReplyAsRead(
    authorization: String?,
    args: MarkCommentReplyAsRead,
  ): Result<SuccessResponse> = retrofitErrorHandler {
    api.markNotificationAsRead(
      generateHeaders(authorization, false),
      MarkNotificationAsRead(
        args.read,
        args.comment_reply_id,
      ),
    )
  }.map {
    SuccessResponse(
      it.success,
    )
  }

  override suspend fun markPersonMentionAsRead(
    authorization: String?,
    args: MarkPersonMentionAsRead,
  ): Result<PersonMentionResponse> = retrofitErrorHandler {
    api.markNotificationAsRead(
      generateHeaders(authorization, false),
      MarkNotificationAsRead(
        args.read,
        args.person_mention_id,
      ),
    )
  }.map {
    PersonMentionResponse(
      null,
    )
  }

  override suspend fun markPrivateMessageAsRead(
    authorization: String?,
    args: MarkPrivateMessageAsRead,
  ): Result<PrivateMessageResponse> = retrofitErrorHandler {
    api.markNotificationAsRead(
      generateHeaders(authorization, false),
      MarkNotificationAsRead(
        args.read,
        args.private_message_id,
      ),
    )
  }.map {
    PrivateMessageResponse(
      null,
    )
  }

  override suspend fun markAllAsRead(
    authorization: String?,
    args: MarkAllAsRead,
  ): Result<SuccessResponse> = retrofitErrorHandler {
    api.markAllAsRead(
      generateHeaders(authorization, false),
    )
  }.map {
    SuccessResponse(
      it.success
    )
  }

  override suspend fun getPersonMentions(
    authorization: String?,
    args: GetPersonMentions,
    force: Boolean,
  ): Result<GetPersonMentionsResponse> = retrofitErrorHandler {
    api.listInbox(
      generateHeaders(authorization, false),
      ListInboxArgs(
        limit = args.limit,
        page_cursor = args.page_cursor,
        creator_id = null,
        unread_only = args.unread_only,
        type = NotificationTypeFilter.mention,
      ).serializeToMap()
    )
  }.map {
    GetPersonMentionsResponse(
      mentions = it.items.map { it.toPersonMentionView() }
    )
  }

  override suspend fun getPrivateMessages(
    authorization: String?,
    args: GetPrivateMessages,
    force: Boolean,
  ): Result<PrivateMessagesResponse> = retrofitErrorHandler {
    api.listInbox(
      generateHeaders(authorization, false),
      ListInboxArgs(
        limit = args.limit,
        page_cursor = args.page_cursor,
        creator_id = null,
        unread_only = args.unread_only,
        type = NotificationTypeFilter.private_message,
      ).serializeToMap()
    )
  }.map {
    PrivateMessagesResponse(
      private_messages = it.items.map { it.toPrivateMessageView() },
      prev_page = it.prevPage,
      next_page = it.nextPage,
    )
  }

  override suspend fun getPrivateMessageReports(
    authorization: String?,
    args: ListPrivateMessageReports,
    force: Boolean,
  ): Result<ListPrivateMessageReportsResponse> = retrofitErrorHandler {
    api.getReports(
      generateHeaders(authorization, false),
      ListReportsI(
        myReportsOnly = true,
        showCommunityRuleViolations = false,
        limit = null,
        pageCursor = args.cursor,
        communityId = null,
        postId = null,
        type = ReportType.private_messages,
        unresolvedOnly = args.unresolved_only,
      ).serializeToMap(),
    )
  }.map {
    ListPrivateMessageReportsResponse(
      private_message_reports = it.items.map { it.toPrivateMessageReportView() },
      prev_page = it.prevPage,
      next_page = it.nextPage,
    )
  }

  override suspend fun createPrivateMessageReport(
    authorization: String?,
    args: CreatePrivateMessageReport,
  ): Result<PrivateMessageReportResponse> = retrofitErrorHandler {
    api.createPrivateMessageReport(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.CreatePrivateMessageReport(
        reason = args.reason,
        privateMessageId = args.private_message_id,
      )
    )
  }.map {
    PrivateMessageReportResponse(
      it.privateMessageReportView.toPrivateMessageReportView()
    )
  }

  override suspend fun resolvePrivateMessageReport(
    authorization: String?,
    args: ResolvePrivateMessageReport,
  ): Result<PrivateMessageReportResponse> = retrofitErrorHandler {
    api.resolvePrivateMessageReport(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.ResolvePrivateMessageReport(
        resolved = args.resolved,
        reportId = args.report_id,
      )
    )
  }.map {
    PrivateMessageReportResponse(
      it.privateMessageReportView.toPrivateMessageReportView()
    )
  }

  override suspend fun getPostReports(
    authorization: String?,
    args: ListPostReports,
    force: Boolean,
  ): Result<ListPostReportsResponse> = retrofitErrorHandler {
    api.getReports(
      generateHeaders(authorization, false),
      ListReportsI(
        myReportsOnly = true,
        showCommunityRuleViolations = false,
        limit = null,
        pageCursor = args.cursor,
        communityId = null,
        postId = null,
        type = ReportType.posts,
        unresolvedOnly = args.unresolved_only,
      ).serializeToMap(),
    )
  }.map {
    ListPostReportsResponse(
      post_reports = it.items.map { it.toPostReportView() },
      prev_page = it.prevPage,
      next_page = it.nextPage,
    )
  }

  override suspend fun resolvePostReport(
    authorization: String?,
    args: ResolvePostReport,
  ): Result<PostReportResponse> = retrofitErrorHandler {
    api.resolvePostReport(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.ResolvePostReport(
        resolved = args.resolved,
        reportId = args.report_id,
      )
    )
  }.map {
    PostReportResponse(
      it.postReportView.toPostReportView()
    )
  }

  override suspend fun getCommentReports(
    authorization: String?,
    args: ListCommentReports,
    force: Boolean,
  ): Result<ListCommentReportsResponse> = retrofitErrorHandler {
    api.getReports(
      generateHeaders(authorization, false),
      ListReportsI(
        myReportsOnly = true,
        showCommunityRuleViolations = false,
        limit = null,
        pageCursor = args.cursor,
        communityId = null,
        postId = null,
        type = ReportType.posts,
        unresolvedOnly = args.unresolved_only,
      ).serializeToMap(),
    )
  }.map {
    ListCommentReportsResponse(
      comment_reports = it.items.map { it.toCommentReportView() },
      prev_page = it.prevPage,
      next_page = it.nextPage,
    )
  }

  override suspend fun resolveCommentReport(
    authorization: String?,
    args: ResolveCommentReport,
  ): Result<CommentReportResponse> = retrofitErrorHandler {
    api.resolveCommentReport(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.ResolveCommentReport(
        resolved = args.resolved,
        reportId = args.report_id,
      )
    )
  }.map {
    CommentReportResponse(
      comment_report_view = it.commentReportView.toCommentReportView()
    )
  }

  override suspend fun createPrivateMessage(
    authorization: String?,
    args: CreatePrivateMessage,
  ): Result<PrivateMessageResponse> = retrofitErrorHandler {
    api.createPrivateMessage(
      generateHeaders(authorization, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.CreatePrivateMessage(
        args.recipient_id,
        args.content,
      )
    )
  }.map {
    PrivateMessageResponse(
      private_message_view = it.privateMessageView.toPrivateMessageView()
    )
  }

  override suspend fun getUnreadCount(
    authorization: String?,
    args: GetUnreadCount,
    force: Boolean,
  ): Result<UnreadCount> = retrofitErrorHandler {
    api.getUnreadCount(
      generateHeaders(authorization, force),
      args.serializeToMap(),
    )
  }.map {
    UnreadCount(
      notificationCount = it.notificationCount.toInt(),
      registrationApplicationCount = it.registrationApplicationCount?.toInt() ?: 0,
      pendingFollowCount = it.pendingFollowCount?.toInt() ?: 0,
      reportCount = it.reportCount?.toInt() ?: 0,

      mentions = null,
      privateMessages = null,
      replies = null,

      commentReports = null,
      postReports = null,
      privateMessageReports = null,
    )
  }

  override suspend fun followCommunity(
    authorization: String?,
    args: FollowCommunity,
  ): Result<CommunityResponse> = retrofitErrorHandler {
    api.followCommunity(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.FollowCommunity(
        follow = args.follow,
        communityId = args.community_id,
      ),
    )
  }.map {
    CommunityResponse(
      community_view = it.communityView.toCommunityView(),
      discussion_languages = it.discussionLanguages
    )
  }

  override suspend fun banUserFromCommunity(
    authorization: String?,
    args: BanFromCommunity,
  ): Result<BanFromCommunityResponse> = retrofitErrorHandler {
    api.banUserFromCommunity(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.BanFromCommunity(
        reason = args.reason ?: "",
        ban = args.ban,
        personId = args.person_id,
        communityId = args.community_id,
        expiresAt = args.expires,
        removeOrRestoreData = args.remove_data,
      ),
    )
  }.map {
    BanFromCommunityResponse(
      person_view = it.personView.toPersonView(),
      banned = it.personView.banned,
    )
  }

  override suspend fun modUser(
    authorization: String?,
    args: AddModToCommunity,
  ): Result<AddModToCommunityResponse> = retrofitErrorHandler {
    api.modUser(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.AddModToCommunity(
        added = args.added,
        personId = args.person_id,
        communityId = args.community_id,
      )
    )
  }.map {
    AddModToCommunityResponse(
      moderators = it.moderators.map { it.toCommunityModeratorView() }
    )
  }

  override suspend fun createPost(
    authorization: String?,
    args: CreatePost,
  ): Result<PostResponse> = retrofitErrorHandler {
    api.createPost(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.CreatePost(
        communityId = args.community_id,
        name = args.name,
        scheduledPublishTimeAt = null,
        tags = null,
        customThumbnail = args.custom_thumbnail,
        languageId = args.language_id,
        nsfw = args.nsfw,
        honeypot = args.honeypot,
        altText = args.alt_text,
        body = args.body,
        url = args.url,
      )
    )
  }.map {
    PostResponse(
      post_view = it.postView.toPostView()
    )
  }

  override suspend fun editPost(
    authorization: String?,
    args: EditPost,
  ): Result<PostResponse> = retrofitErrorHandler {
    api.editPost(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.EditPost(
        postId = args.post_id,
        tags = null,
        scheduledPublishTimeAt = null,
        customThumbnail = args.custom_thumbnail,
        languageId = args.language_id,
        nsfw = args.nsfw,
        altText = args.alt_text,
        body = args.body,
        url = args.url,
        name = args.name,
      )
    )
  }.map {
    PostResponse(post_view = it.postView.toPostView())
  }

  override suspend fun deletePost(
    authorization: String?,
    args: DeletePost,
  ): Result<PostResponse> = retrofitErrorHandler {
    api.deletePost(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.DeletePost(
        deleted = args.deleted,
        postId = args.post_id,
      ),
    )
  }.map {
    PostResponse(post_view = it.postView.toPostView())
  }

  override suspend fun featurePost(
    authorization: String?,
    args: FeaturePost,
  ): Result<PostResponse> = retrofitErrorHandler {
    api.featurePost(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.FeaturePost(
        featureType = when (args.feature_type) {
          PostFeatureType.Local -> com.idunnololz.summit.api.dto.lemmy.v4.models.PostFeatureType.local
          PostFeatureType.Community -> com.idunnololz.summit.api.dto.lemmy.v4.models.PostFeatureType.community
        },
        featured = args.featured,
        postId = args.post_id,
      )
    )
  }.map {
    PostResponse(post_view = it.postView.toPostView())
  }

  override suspend fun lockPost(
    authorization: String?,
    args: LockPost,
  ): Result<PostResponse> = retrofitErrorHandler {
    api.lockPost(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.LockPost(
        reason = "",
        locked = args.locked,
        postId = args.post_id,
      )
    )
  }.map {
    PostResponse(post_view = it.postView.toPostView())
  }

  override suspend fun removePost(
    authorization: String?,
    args: RemovePost,
  ): Result<PostResponse> = retrofitErrorHandler {
    api.removePost(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.RemovePost(
        reason = args.reason ?: "",
        removed = args.removed,
        postId = args.post_id,
        removeChildren = null,
      )
    )
  }.map {
    PostResponse(post_view = it.postView.toPostView())
  }

  override suspend fun search(
    authorization: String?,
    args: Search,
    force: Boolean,
  ): Result<SearchResponse> = retrofitErrorHandler {
    api.search(
      generateHeaders(authorization, force),
      SearchI(
        searchTerm = args.q,
        limit = args.limit,
        pageCursor = args.page_cursor,
        showNsfw = null,
        postUrlOnly = null,
        titleOnly = null,
        listingType = args.listing_type?.toType(),
        timeRangeSeconds = args.sort?.toTimeInSeconds(),
        type = args.type_?.toSearchType(),
        creatorUsername = null,
        creatorId = args.creator_id,
        communityName = args.community_name,
        communityId = args.community_id,
      ).serializeToMap(),
    )
  }.map {
    SearchResponse(
      type_ = args.type_,
      comments = it.comments.map { it.toCommentView() },
      posts = it.posts.map { it.toPostView() },
      communities = it.communities.map { it.toCommunityView() },
      users = it.persons.map { it.toPersonView() },
      nextCursor = it.nextPage,
      prevCursor = it.prevPage,
    )
  }

  override suspend fun getSiteMetadata(
    authorization: String?,
    args: GetSiteMetadata,
    force: Boolean,
  ): Result<GetSiteMetadataResponse> = retrofitErrorHandler {
    api.getSiteMetadata(
      generateHeaders(authorization, force),
      GetSiteMetadataI(
        args.url
      ).serializeToMap(),
    )
  }.map {
    GetSiteMetadataResponse(
      it.metadata.toSiteMetadata()
    )
  }

  override suspend fun createCommentReport(
    authorization: String?,
    args: CreateCommentReport,
  ): Result<CommentReportResponse> = retrofitErrorHandler {
    api.createCommentReport(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.CreateCommentReport(
        reason = args.reason,
        commentId = args.comment_id,
        violatesInstanceRules = null,
      )
    )
  }.map {
    CommentReportResponse(
      comment_report_view = it.commentReportView.toCommentReportView(),
    )
  }

  override suspend fun createPostReport(
    authorization: String?,
    args: CreatePostReport,
  ): Result<PostReportResponse> = retrofitErrorHandler {
    api.createPostReport(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.CreatePostReport(
        reason = args.reason,
        postId = args.post_id,
        violatesInstanceRules = null,
      )
    )
  }.map {
    PostReportResponse(
      it.postReportView.toPostReportView(),
    )
  }

  override suspend fun blockPerson(
    authorization: String?,
    args: BlockPerson,
  ): Result<BlockPersonResponse> = retrofitErrorHandler {
    api.blockPerson(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.BlockPerson(
        block = args.block,
        personId = args.person_id,
      )
    )
  }.map {
    BlockPersonResponse(
      person_view = it.personView.toPersonView(),
      blocked = it.personView.personActions?.blockedAt != null,
    )
  }

  override suspend fun blockCommunity(
    authorization: String?,
    args: BlockCommunity,
  ): Result<BlockCommunityResponse> = retrofitErrorHandler {
    api.blockCommunity(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.BlockCommunity(
        args.block,
        args.community_id,
      )
    )
  }.map {
    BlockCommunityResponse(
      community_view = it.data.communityView.toCommunityView(),
      blocked = it.data.communityView.communityActions?.blockedAt != null
    )
  }

  override suspend fun blockInstance(
    authorization: String?,
    args: BlockInstance,
  ): Result<BlockInstanceResponse> = retrofitErrorHandler {
    api.blockInstance(
      generateHeaders(authorization, force = false),
      UserBlockInstanceCommunitiesParams(
        block = args.block,
        instanceId = args.instance_id,
      )
    )
  }.map {
    BlockInstanceResponse(
      blocked = args.block,
    )
  }

  override suspend fun saveUserSettings(
    authorization: String?,
    args: SaveUserSettings,
  ): Result<Unit> = retrofitErrorHandler {

    /*
  val default_sort_type: SortType? /* "Active" | "Hot" | "New" | "Old" | "TopDay" | "TopWeek" | "TopMonth" | "TopYear" | "TopAll" | "MostComments" | "NewComments" */ =
    null,
  val default_listing_type: ListingType? /* "All" | "Local" | "Subscribed" */ = null,
  val interface_language: String? = null,
  val avatar: String? = null,
  val banner: String? = null,
  val : Boolean? = null,
  val show_new_post_notifs: Boolean? = null,
  val generate_totp_2fa: Boolean? = null,
     */

    api.saveUserSettings(
      generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.SaveUserSettings(
        showPersonVotes = null,
        hidePostsWithMedia = null,
        autoMarkFetchedPostsAsRead = null,
        showUpvotePercentage = null,
        showDownvotes = null,
        showUpvotes = null,
        showScore = args.show_scores,
        collapseBotComments = null,
        privateMessagesEnabled = null,
        animatedImagesEnabled = null,
        infiniteScrollEnabled = null,
        openLinksInNewTab = null,
        blockingKeywords = null,
        discussionLanguages = args.discussion_languages,
        showReadPosts = args.show_read_posts,
        showBotAccounts = args.show_bot_accounts,
        botAccount = args.bot_account,
        sendNotificationsToEmail = args.send_notifications_to_email,
        showMedia = null,
        showAvatars = args.show_avatars,
        matrixUserId = args.matrix_user_id,
        bio = args.bio,
        email = args.email,
        displayName = args.display_name,
        interfaceLanguage = null,
        defaultCommentSortType = null,
        defaultItemsPerPage = null,
        defaultPostTimeRangeSeconds = null,
        defaultPostSortType = null,
        postListingMode = null,
        defaultListingType = null,
        theme = args.theme,
        blurNsfw = null,
        showNsfw = args.show_nsfw,
      ),
    )
  }.map {

  }

  override suspend fun uploadImage(
    authorization: String?,
    url: String,
    fileName: String,
    imageIs: InputStream,
    mimeType: String?,
  ): Result<UploadImageResult> = retrofitErrorHandler {
    api.uploadImage(
      headers = generateHeaders(authorization, force = false),
      token = "jwt=$authorization",
      url = url,
      filePart = MultipartBody.Part.createFormData(
        name = "images[]",
        filename = fileName,
        body = imageIs.readBytes().toRequestBody(contentType = mimeType?.toMediaType()),
      ),
    )
  }.map {
    UploadImageResult(
      it.imageUrl,
    )
  }

  override suspend fun resolveObject(
    authorization: String?,
    args: ResolveObject,
    force: Boolean,
  ): Result<ResolveObjectResponse> = retrofitErrorHandler {
    api.resolveObject(
      headers = generateHeaders(authorization, force = force),
      form = ResolveObjectI(
        args.q,
      ).serializeToMap(),
    )
  }.map {
    it.toResolveObjectResponse()
  }

  override suspend fun banUserFromSite(
    authorization: String?,
    args: BanPerson,
  ): Result<BanPersonResponse> = retrofitErrorHandler {
    api.banUserFromSite(
      headers = generateHeaders(authorization, force = false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.BanPerson(
        reason = args.reason ?: "",
        ban = args.ban,
        personId = args.person_id,
        expiresAt = args.expires,
        removeOrRestoreData = null,
      )
    )
  }.map {
    BanPersonResponse(
      person_view = it.personView.toPersonView(),
      banned = it.personView.banned,
    )
  }

  override suspend fun removeCommunity(
    authorization: String?,
    args: RemoveCommunity,
  ): Result<CommunityResponse> = retrofitErrorHandler {
    api.removeCommunity(
      headers = generateHeaders(authorization, force = false),
      form = com.idunnololz.summit.api.dto.lemmy.v4.models.RemoveCommunity(
        reason = args.reason ?: "",
        removed = args.removed,
        communityId = args.community_id,
      )
    )
  }.map {
    CommunityResponse(
      community_view = it.communityView.toCommunityView(),
      discussion_languages = it.discussionLanguages,
    )
  }

  override suspend fun hideCommunity(
    authorization: String?,
    args: HideCommunity,
  ): Result<SuccessResponse> = retrofitErrorHandler {
    api.hideCommunity(
      headers = generateHeaders(authorization, force = false),
      form = com.idunnololz.summit.api.dto.lemmy.v4.models.HideCommunity(
        reason = args.reason ?: "",
        hidden = args.hidden,
        communityId = args.community_id,
      ),
    )
  }.map {
    SuccessResponse(it.success)
  }

  override suspend fun purgePerson(
    authorization: String?,
    args: PurgePerson,
  ): Result<SuccessResponse> = retrofitErrorHandler {
    api.purgePerson(
      headers = generateHeaders(authorization, force = false),
      form = com.idunnololz.summit.api.dto.lemmy.v4.models.PurgePerson(
        reason = args.reason ?: "",
        personId = args.person_id,
      ),
    )
  }.map {
    SuccessResponse(it.success)
  }

  override suspend fun purgeCommunity(
    authorization: String?,
    args: PurgeCommunity,
  ): Result<SuccessResponse> = retrofitErrorHandler {
    api.purgeCommunity(
      headers = generateHeaders(authorization, force = false),
      form = com.idunnololz.summit.api.dto.lemmy.v4.models.PurgeCommunity(
        reason = args.reason ?: "",
        communityId = args.community_id,
      ),
    )
  }.map {
    SuccessResponse(it.success)
  }

  override suspend fun purgePost(
    authorization: String?,
    args: PurgePost,
  ): Result<SuccessResponse> = retrofitErrorHandler {
    api.purgePost(
      headers = generateHeaders(authorization, force = false),
      form = com.idunnololz.summit.api.dto.lemmy.v4.models.PurgePost(
        reason = args.reason ?: "",
        postId = args.post_id,
      ),
    )
  }.map {
    SuccessResponse(it.success)
  }

  override suspend fun purgeComment(
    authorization: String?,
    args: PurgeComment,
  ): Result<SuccessResponse> = retrofitErrorHandler {
    api.purgeComment(
      headers = generateHeaders(authorization, force = false),
      form = com.idunnololz.summit.api.dto.lemmy.v4.models.PurgeComment(
        reason = args.reason ?: "",
        commentId = args.comment_id
      ),
    )
  }.map {
    SuccessResponse(it.success)
  }

  override suspend fun listRegistrationApplications(
    authorization: String?,
    args: ListRegistrationApplications,
    force: Boolean,
  ): Result<PagedResponseRegistrationApplicationView> = retrofitErrorHandler {
    api.listRegistrationApplications(
      headers = generateHeaders(authorization, force = false),
      form = ListRegistrationApplicationsI(
        limit = args.limit,
        pageCursor = args.pageCursor,
        unreadOnly = args.unread_only,
      ).serializeToMap()
    )
  }.map {
    PagedResponseRegistrationApplicationView(
      items = it.items.map { it.toUserRegistrationApplication(instance) },
      prevPage = it.prevPage,
      nextPage = it.nextPage,
    )
  }

  override suspend fun approveRegistrationApplication(
    authorization: String?,
    args: ApproveRegistrationApplication,
  ): Result<Unit> = retrofitErrorHandler {
    api.approveRegistrationApplication(
      headers = generateHeaders(authorization, force = false),
      form = com.idunnololz.summit.api.dto.lemmy.v4.models.ApproveRegistrationApplication(
        approve = args.approve,
        id = args.id,
        denyReason = args.deny_reason,
      )
    )
  }.map {

  }

  override suspend fun getModLogs(
    authorization: String?,
    args: GetModlog,
    force: Boolean,
  ): Result<GetModlogResponse> = retrofitErrorHandler {
    api.getModLogs(
      headers = generateHeaders(authorization, force = false),
      form = GetModlogI(
        limit = args.limit,
        pageCursor = args.page_cursor,
        bulkActionParentId = null,
        showBulk = null,
        commentId = args.comment_id,
        postId = args.post_id,
        otherPersonId = args.other_person_id,
        listingType = null,
        type = args.type_.toModlogKindFilter(),
        communityId = args.community_id,
        modPersonId = args.mod_person_id,
      ).serializeToMap()
    )
  }.map {
    GetModlogResponse(
      modEvents = it.items.map { it.toModEvent() },
      nextPage = it.nextPage,
      prevPage = it.prevPage,
    )
  }

  override suspend fun register(args: Register): Result<LoginResponse> = retrofitErrorHandler {
    api.register(
      generateHeaders(null, false),
      com.idunnololz.summit.api.dto.lemmy.v4.models.Register(
        passwordVerify = args.password_verify,
        password = args.password,
        username = args.username,
        token = null,
        stayLoggedIn = null,
        answer = args.answer,
        honeypot = args.honeypot,
        captchaAnswer = args.captcha_answer,
        captchaUuid = args.captcha_uuid,
        email = args.email,
        showNsfw = args.show_nsfw
      )
    )
  }.map {
    LoginResponse(
      jwt = it.jwt,
      registration_created = it.registrationCreated,
      verify_email_sent = it.verifyEmailSent,
    )
  }

  override suspend fun getCaptcha(): Result<GetCaptchaResponse> = retrofitErrorHandler {
    api.getCaptcha(
      headers = generateHeaders(authorization = null, force = false),
    )
  }.map {
    GetCaptchaResponse(
      ok = it.ok?.let {
        CaptchaResponse(
          png = it.png,
          wav = it.wav,
          uuid = it.uuid,
        )
      }
    )
  }

  override suspend fun listMedia(
    authorization: String?,
    args: ListMedia,
    force: Boolean,
  ): Result<ListMediaResponse> = retrofitErrorHandler {
    api.listMedia(
      headers = generateHeaders(authorization, force),
      form = ListMediaI(
        args.limit?.toInt(),
        args.pageCursor,
      ).serializeToMap()
    )
  }.map {
    ListMediaResponse(
      images = it.items.map {
        LocalImageView(
          LocalImage(
            local_user_id = it.localImage.personId,
            pictrs_alias = it.localImage.pictrsAlias,
            pictrs_delete_token = null,
            published = it.localImage.publishedAt,
          ),
          person = it.person.toPerson(false, null, false)
        )
      },
      nextPage = it.nextPage,
      prevPage = it.prevPage,
    )
  }

  override suspend fun federatedInstances(
    authorization: String?,
    force: Boolean,
  ): Result<FederatedInstances> = retrofitErrorHandler {
    api.federatedInstances(generateHeaders(authorization, force))
  }.map {
    val linked = mutableListOf<Instance>()
    val allowed = mutableListOf<Instance>()
    val blocked = mutableListOf<Instance>()

    for (instanceView in it.items) {
      if (instanceView.allowed != null) {
        allowed.add(instanceView.instance.toInstance())
      } else if (instanceView.blocked != null) {
        blocked.add(instanceView.instance.toInstance())
      } else {
        linked.add(instanceView.instance.toInstance())
      }
    }

    FederatedInstances(
      linked = linked,
      allowed = allowed,
      blocked = blocked,
    )
  }

  override suspend fun deleteMedia(
    authorization: String?,
    args: DeleteImage,
  ): Result<Unit> = retrofitErrorHandler {
    api.deleteMedia(
      headers = generateHeaders(authorization, force = false),
      body = DeleteImageParamsI(
        filename = args.filename
      ),
    )
  }


  private fun <T> T.serializeToMap(): Map<String, String> = convert()

  // convert an object of type I to type O
  private inline fun <I, reified O> I.convert(): O {
    val json = gson.toJson(this)
    return gson.fromJson(
      json,
      object : TypeToken<O>() {}.type,
    )
  }
}
