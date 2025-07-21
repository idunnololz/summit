package com.idunnololz.summit.api

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.idunnololz.summit.api.converters.toCommentReportView
import com.idunnololz.summit.api.converters.toCommentResponse
import com.idunnololz.summit.api.converters.toCommentView
import com.idunnololz.summit.api.converters.toCommunityModeratorView
import com.idunnololz.summit.api.converters.toCommunityResponse
import com.idunnololz.summit.api.converters.toCommunityView
import com.idunnololz.summit.api.converters.toCreateComment
import com.idunnololz.summit.api.converters.toCreatePost
import com.idunnololz.summit.api.converters.toEditComment
import com.idunnololz.summit.api.converters.toEditPost
import com.idunnololz.summit.api.converters.toGetPersonMentionsResponse
import com.idunnololz.summit.api.converters.toGetRepliesResponse
import com.idunnololz.summit.api.converters.toLoginResponse
import com.idunnololz.summit.api.converters.toMyUserInfo
import com.idunnololz.summit.api.converters.toPersonView
import com.idunnololz.summit.api.converters.toPostReportView
import com.idunnololz.summit.api.converters.toPostResponse
import com.idunnololz.summit.api.converters.toPostView
import com.idunnololz.summit.api.converters.toPrivateMessageView
import com.idunnololz.summit.api.converters.toSearchType
import com.idunnololz.summit.api.converters.toSite
import com.idunnololz.summit.api.converters.toSortType
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
import com.idunnololz.summit.api.dto.lemmy.DeletePost
import com.idunnololz.summit.api.dto.lemmy.DistinguishComment
import com.idunnololz.summit.api.dto.lemmy.EditComment
import com.idunnololz.summit.api.dto.lemmy.EditCommunity
import com.idunnololz.summit.api.dto.lemmy.EditPost
import com.idunnololz.summit.api.dto.lemmy.FeaturePost
import com.idunnololz.summit.api.dto.lemmy.FollowCommunity
import com.idunnololz.summit.api.dto.lemmy.GetCaptchaResponse
import com.idunnololz.summit.api.dto.lemmy.GetComments
import com.idunnololz.summit.api.dto.lemmy.GetCommentsResponse
import com.idunnololz.summit.api.dto.lemmy.GetCommunity
import com.idunnololz.summit.api.dto.lemmy.GetCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.GetModlog
import com.idunnololz.summit.api.dto.lemmy.GetModlogResponse
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
import com.idunnololz.summit.api.dto.lemmy.GetReportCount
import com.idunnololz.summit.api.dto.lemmy.GetReportCountResponse
import com.idunnololz.summit.api.dto.lemmy.GetSite
import com.idunnololz.summit.api.dto.lemmy.GetSiteMetadata
import com.idunnololz.summit.api.dto.lemmy.GetSiteMetadataResponse
import com.idunnololz.summit.api.dto.lemmy.GetSiteResponse
import com.idunnololz.summit.api.dto.lemmy.GetUnreadCount
import com.idunnololz.summit.api.dto.lemmy.GetUnreadCountResponse
import com.idunnololz.summit.api.dto.lemmy.GetUnreadRegistrationApplicationCount
import com.idunnololz.summit.api.dto.lemmy.GetUnreadRegistrationApplicationCountResponse
import com.idunnololz.summit.api.dto.lemmy.HideCommunity
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
import com.idunnololz.summit.api.dto.lemmy.ListRegistrationApplicationsResponse
import com.idunnololz.summit.api.dto.lemmy.LocalSite
import com.idunnololz.summit.api.dto.lemmy.LockPost
import com.idunnololz.summit.api.dto.lemmy.Login
import com.idunnololz.summit.api.dto.lemmy.LoginResponse
import com.idunnololz.summit.api.dto.lemmy.MarkAllAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkCommentReplyAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkPersonMentionAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkPostAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkPrivateMessageAsRead
import com.idunnololz.summit.api.dto.lemmy.PersonMentionResponse
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
import com.idunnololz.summit.api.dto.lemmy.RegistrationApplicationResponse
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
import com.idunnololz.summit.api.dto.lemmy.SearchType
import com.idunnololz.summit.api.dto.lemmy.SiteAggregates
import com.idunnololz.summit.api.dto.lemmy.SiteView
import com.idunnololz.summit.api.dto.lemmy.SuccessResponse
import com.idunnololz.summit.api.dto.piefed.CommentView
import com.idunnololz.summit.api.dto.piefed.GetComment
import java.io.InputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class PieFedApiAlphaAdapter(
  private val api: PieFedApiAlpha,
  override val instance: String,
) : LemmyLikeApi {

  companion object {
    private val gson = GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create()
  }

  override fun supportsFeature(apiFeature: ApiFeature): Boolean = when (apiFeature) {
    ApiFeature.Reports -> false
    ApiFeature.Register -> false
    ApiFeature.Downvoted -> false
  }

  private fun generateHeaders(authorization: String?, force: Boolean): Map<String, String> {
    val headers = mutableMapOf<String, String>()
    if (authorization != null) {
      headers["Authorization"] = authorization
    }
    if (force) {
      headers[CACHE_CONTROL_HEADER] = CACHE_CONTROL_NO_CACHE
    }
    return headers
  }

  fun <T> T.serializeToMap(): Map<String, String> {
    return convert()
  }

  // convert an object of type I to type O
  private inline fun <I, reified O> I.convert(): O {
    val json = gson.toJson(this)
    return gson.fromJson(
      json,
      object : TypeToken<O>() {}.type,
    )
  }

  override suspend fun getSite(
    authorization: String?,
    args: GetSite,
    force: Boolean,
  ): Result<GetSiteResponse> = retrofitErrorHandler {
    api.getSite(generateHeaders(authorization, force), args.serializeToMap())
  }.map {
    GetSiteResponse(
      site_view = SiteView(
        site = it.site.toSite(),
        local_site = LocalSite(),
        local_site_rate_limit = null,
        counts = SiteAggregates(
          id = null,
          site_id = null,
          users = null,
          posts = null,
          comments = null,
          communities = null,
          users_active_day = null,
          users_active_week = null,
          users_active_month = null,
          users_active_half_year = null,
        ),
      ),
      admins = it.admins.map { it.toPersonView() },
      online = null,
      version = it.version,
      my_user = it.myUser?.toMyUserInfo(),
      all_languages = listOf(),
      discussion_languages = listOf(),
      taglines = listOf(),
      custom_emojis = listOf(),
    )
  }

  override suspend fun getPosts(
    authorization: String?,
    args: GetPosts,
    force: Boolean,
  ): Result<GetPostsResponse> = retrofitErrorHandler {
    api.getPosts(
      generateHeaders(authorization, force),
      args.serializeToMap(),
    )
  }
    .map {
      GetPostsResponse(
        posts = it.posts.map { it.toPostView() },
        next_page = null,
      )
    }

  override suspend fun getPost(
    authorization: String?,
    args: GetPost,
    force: Boolean,
  ): Result<GetPostResponse> {
    val headers = generateHeaders(authorization, force)

    if (args.id == null && args.comment_id != null) {
      return retrofitErrorHandler {
        api.getComment(headers, GetComment(args.comment_id).serializeToMap())
      }.fold(
        onSuccess = {
          getPost(
            authorization,
            args.copy(id = it.commentView.post.id),
            force,
          )
        },
        onFailure = {
          Result.failure(it)
        }
      )
    }

    return retrofitErrorHandler {
      api.getPost(
        headers,
        args.serializeToMap(),
      )
    }
      .map {
        GetPostResponse(
          post_view = it.postView.toPostView(),
          community_view = it.communityView.toCommunityView(),
          moderators = it.moderators.map { it.toCommunityModeratorView() },
          cross_posts = it.crossPosts.map { it.toPostView() },
          online = 0,
        )
      }
  }

  override suspend fun login(args: Login): Result<LoginResponse> = retrofitErrorHandler {
    api.login(
      headers = generateHeaders(null, false),
      form = com.idunnololz.summit.api.dto.piefed.Login(
        args.username_or_email,
        args.password,
      ),
    )
  }.map { it.toLoginResponse() }

  override suspend fun likePost(
    authorization: String?,
    args: CreatePostLike,
  ): Result<PostResponse> =
    retrofitErrorHandler { api.likePost(generateHeaders(authorization, false), args) }
      .map { it.toPostResponse() }

  override suspend fun likeComment(
    authorization: String?,
    args: CreateCommentLike,
  ): Result<CommentResponse> =
    retrofitErrorHandler { api.likeComment(generateHeaders(authorization, false), args) }
      .map { it.toCommentResponse() }

  override suspend fun listCommentVotes(
    authorization: String?,
    args: ListCommentLikes,
    force: Boolean,
  ): Result<ListCommentLikesResponse> = retrofitErrorHandler {
    api.listCommentVotes(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun listPostVotes(
    authorization: String?,
    args: ListPostLikes,
    force: Boolean,
  ): Result<ListPostLikesResponse> = retrofitErrorHandler {
    api.listPostVotes(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun createComment(
    authorization: String?,
    args: CreateComment,
  ): Result<CommentResponse> = retrofitErrorHandler {
    api.createComment(generateHeaders(authorization, false), args.toCreateComment())
  }.map { it.toCommentResponse() }

  override suspend fun editComment(
    authorization: String?,
    args: EditComment,
  ): Result<CommentResponse> =
    retrofitErrorHandler {
      api.editComment(generateHeaders(authorization, false), args.toEditComment())
    }.map { it.toCommentResponse() }

  override suspend fun deleteComment(
    authorization: String?,
    args: DeleteComment,
  ): Result<CommentResponse> =
    retrofitErrorHandler { api.deleteComment(generateHeaders(authorization, false), args) }
      .map { it.toCommentResponse() }

  override suspend fun savePost(authorization: String?, args: SavePost): Result<PostResponse> =
    retrofitErrorHandler { api.savePost(generateHeaders(authorization, false), args) }
      .map { it.toPostResponse() }

  override suspend fun markPostAsRead(
    authorization: String?,
    args: MarkPostAsRead,
  ): Result<SuccessResponse> =
    retrofitErrorHandler { api.markPostAsRead(generateHeaders(authorization, false), args) }

  override suspend fun saveComment(
    authorization: String?,
    args: SaveComment,
  ): Result<CommentResponse> =
    retrofitErrorHandler { api.saveComment(generateHeaders(authorization, false), args) }
      .map { it.toCommentResponse() }

  override suspend fun getComments(
    authorization: String?,
    args: GetComments,
    force: Boolean,
  ): Result<GetCommentsResponse> {
    val limit = args.limit
    val headers = generateHeaders(authorization = authorization, force = force)

    suspend fun getResult(): Result<com.idunnololz.summit.api.dto.piefed.GetCommentsResponse> {
      if (limit != null) {
        return retrofitErrorHandler {
          api.getComments(
            headers = headers,
            form = args.serializeToMap(),
          )
        }
      }

      val limit = 100
      val comments = mutableListOf<CommentView>()
      var page = 0
      while (true) {
        val result = retrofitErrorHandler {
          api.getComments(
            headers = headers,
            form = args.copy(page = page, limit = limit).serializeToMap(),
          )
        }

        if (result.isFailure) {
          return result
        }
        val cs = result.getOrThrow().comments
        comments.addAll(cs)

        if (cs.size < limit) {
          break
        }
        page++
      }

      return Result.success(com.idunnololz.summit.api.dto.piefed.GetCommentsResponse(comments))
    }

    val result = getResult()

    return result.map {
      GetCommentsResponse(
        it.comments.map { it.toCommentView() },
      )
    }
  }

  override suspend fun distinguishComment(
    authorization: String?,
    args: DistinguishComment,
  ): Result<CommentResponse> =
    retrofitErrorHandler { api.distinguishComment(generateHeaders(authorization, false), args) }
      .map { it.toCommentResponse() }

  override suspend fun removeComment(
    authorization: String?,
    args: RemoveComment,
  ): Result<CommentResponse> =
    retrofitErrorHandler { api.removeComment(generateHeaders(authorization, false), args) }
      .map { it.toCommentResponse() }

  override suspend fun getCommunity(
    authorization: String?,
    args: GetCommunity,
    force: Boolean,
  ): Result<GetCommunityResponse> = retrofitErrorHandler {
    api.getCommunity(
      headers = generateHeaders(authorization, force),
      form = args.serializeToMap(),
    )
  }.map {
    GetCommunityResponse(
      community_view = it.communityView.toCommunityView(),
      site = it.site?.toSite(),
      moderators = it.moderators.map { it.toCommunityModeratorView() },
      online = null,
      discussion_languages = it.discussionLanguages,
      default_post_language = null,
    )
  }

  override suspend fun createCommunity(
    authorization: String?,
    args: CreateCommunity,
  ): Result<CommunityResponse> = retrofitErrorHandler {
    api.createCommunity(
      headers = generateHeaders(authorization = authorization, force = false),
      createCommunity = args,
    )
  }.map { it.toCommunityResponse() }

  override suspend fun updateCommunity(
    authorization: String?,
    args: EditCommunity,
  ): Result<CommunityResponse> = retrofitErrorHandler {
    api.updateCommunity(
      headers = generateHeaders(authorization = authorization, force = false),
      editCommunity = args,
    )
  }.map { it.toCommunityResponse() }

  override suspend fun deleteCommunity(
    authorization: String?,
    args: DeleteCommunity,
  ): Result<CommunityResponse> = retrofitErrorHandler {
    api.deleteCommunity(
      headers = generateHeaders(authorization, false),
      deleteCommunity = args,
    )
  }.map { it.toCommunityResponse() }

  override suspend fun getCommunityList(
    authorization: String?,
    args: ListCommunities,
    force: Boolean,
  ): Result<ListCommunitiesResponse> = retrofitErrorHandler {
    api.getCommunityList(
      headers = generateHeaders(authorization, force),
      form = args.serializeToMap(),
    )
  }.map {
    ListCommunitiesResponse(
      it.communities.map { it.toCommunityView() },
    )
  }

  override suspend fun getPersonDetails(
    authorization: String?,
    args: GetPersonDetails,
    force: Boolean,
  ): Result<GetPersonDetailsResponse> = retrofitErrorHandler {
    api.getPersonDetails(
      generateHeaders(authorization = authorization, force = force),
      com.idunnololz.summit.api.dto.piefed.GetPersonDetails(
        personId = args.person_id?.toInt(),
        username = args.username,
        sort = args.sort?.toSortType(),
        page = args.page,
        limit = args.limit,
        communityId = args.community_id,
        savedOnly = args.saved_only,
        includeContent = args.include_content,
      ).serializeToMap(),
    )
  }.map {
    GetPersonDetailsResponse(
      person_view = it.personView.toPersonView(),
      comments = it.comments.map { it.toCommentView() },
      posts = it.posts.map { it.toPostView() },
      moderates = it.moderates.map { it.toCommunityModeratorView() },
    )
  }

  override suspend fun changePassword(
    authorization: String?,
    args: ChangePassword,
  ): Result<LoginResponse> =
    retrofitErrorHandler { api.changePassword(generateHeaders(authorization, false), args) }
      .map { it.toLoginResponse() }

  override suspend fun getReplies(
    authorization: String?,
    args: GetReplies,
    force: Boolean,
  ): Result<GetRepliesResponse> = retrofitErrorHandler {
    api.getReplies(
      headers = generateHeaders(authorization, force),
      form = args.serializeToMap(),
    )
  }.map { it.toGetRepliesResponse() }

  override suspend fun markCommentReplyAsRead(
    authorization: String?,
    args: MarkCommentReplyAsRead,
  ): Result<SuccessResponse> =
    retrofitErrorHandler { api.markCommentReplyAsRead(generateHeaders(authorization, false), args) }

  override suspend fun markPersonMentionAsRead(
    authorization: String?,
    args: MarkPersonMentionAsRead,
  ): Result<PersonMentionResponse> = retrofitErrorHandler {
    api.markPersonMentionAsRead(
      generateHeaders(authorization, false),
      args,
    )
  }

  override suspend fun markPrivateMessageAsRead(
    authorization: String?,
    args: MarkPrivateMessageAsRead,
  ): Result<PrivateMessageResponse> = retrofitErrorHandler {
    api.markPrivateMessageAsRead(
      generateHeaders(authorization, false),
      args,
    )
  }.map {
    PrivateMessageResponse(
      it.privateMessageView.toPrivateMessageView(),
    )
  }

  override suspend fun markAllAsRead(
    authorization: String?,
    args: MarkAllAsRead,
  ): Result<GetRepliesResponse> =
    retrofitErrorHandler { api.markAllAsRead(generateHeaders(authorization, false), args) }
      .map { it.toGetRepliesResponse() }

  override suspend fun getPersonMentions(
    authorization: String?,
    args: GetPersonMentions,
    force: Boolean,
  ): Result<GetPersonMentionsResponse> = retrofitErrorHandler {
    api.getPersonMentions(generateHeaders(authorization, force), args.serializeToMap())
  }
    .map { it.toGetPersonMentionsResponse() }

  override suspend fun getPrivateMessages(
    authorization: String?,
    args: GetPrivateMessages,
    force: Boolean,
  ): Result<PrivateMessagesResponse> = retrofitErrorHandler {
    api.getPrivateMessages(generateHeaders(authorization, force), args.serializeToMap())
  }.map {
    PrivateMessagesResponse(
      private_messages = it.privateMessages.map { it.toPrivateMessageView() },
    )
  }

  override suspend fun getPrivateMessageReports(
    authorization: String?,
    args: ListPrivateMessageReports,
    force: Boolean,
  ): Result<ListPrivateMessageReportsResponse> = retrofitErrorHandler {
    api.getPrivateMessageReports(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun createPrivateMessageReport(
    authorization: String?,
    args: CreatePrivateMessageReport,
  ): Result<PrivateMessageReportResponse> = retrofitErrorHandler {
    api.createPrivateMessageReport(
      generateHeaders(authorization, false),
      args,
    )
  }

  override suspend fun resolvePrivateMessageReport(
    authorization: String?,
    args: ResolvePrivateMessageReport,
  ): Result<PrivateMessageReportResponse> = retrofitErrorHandler {
    api.resolvePrivateMessageReport(generateHeaders(authorization, false), args)
  }

  override suspend fun getPostReports(
    authorization: String?,
    args: ListPostReports,
    force: Boolean,
  ): Result<ListPostReportsResponse> = retrofitErrorHandler {
    api.getPostReports(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun resolvePostReport(
    authorization: String?,
    args: ResolvePostReport,
  ): Result<PostReportResponse> =
    retrofitErrorHandler { api.resolvePostReport(generateHeaders(authorization, false), args) }
      .map {
        PostReportResponse(
          post_report_view = it.postReportView.toPostReportView(),
        )
      }

  override suspend fun getCommentReports(
    authorization: String?,
    args: ListCommentReports,
    force: Boolean,
  ): Result<ListCommentReportsResponse> = retrofitErrorHandler {
    api.getCommentReports(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun resolveCommentReport(
    authorization: String?,
    args: ResolveCommentReport,
  ): Result<CommentReportResponse> =
    retrofitErrorHandler { api.resolveCommentReport(generateHeaders(authorization, false), args) }
      .map {
        CommentReportResponse(
          comment_report_view = it.commentReportView.toCommentReportView(),
        )
      }

  override suspend fun createPrivateMessage(
    authorization: String?,
    args: CreatePrivateMessage,
  ): Result<PrivateMessageResponse> =
    retrofitErrorHandler { api.createPrivateMessage(generateHeaders(authorization, false), args) }
      .map {
        PrivateMessageResponse(
          private_message_view = it.privateMessageView.toPrivateMessageView(),
        )
      }

  override suspend fun getUnreadCount(
    authorization: String?,
    args: GetUnreadCount,
    force: Boolean,
  ): Result<GetUnreadCountResponse> = retrofitErrorHandler {
    api.getUnreadCount(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun getReportCount(
    authorization: String?,
    args: GetReportCount,
    force: Boolean,
  ): Result<GetReportCountResponse> = retrofitErrorHandler {
    api.getReportCount(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun followCommunity(
    authorization: String?,
    args: FollowCommunity,
  ): Result<CommunityResponse> = retrofitErrorHandler {
    api.followCommunity(
      headers = generateHeaders(authorization, false),
      form = args,
    )
  }.map { it.toCommunityResponse() }

  override suspend fun banUserFromCommunity(
    authorization: String?,
    args: BanFromCommunity,
  ): Result<BanFromCommunityResponse> =
    retrofitErrorHandler { api.banUserFromCommunity(generateHeaders(authorization, false), args) }

  override suspend fun modUser(
    authorization: String?,
    args: AddModToCommunity,
  ): Result<AddModToCommunityResponse> =
    retrofitErrorHandler { api.modUser(generateHeaders(authorization, false), args) }
      .map {
        AddModToCommunityResponse(
          moderators = it.moderators.map { it.toCommunityModeratorView() },
        )
      }

  override suspend fun createPost(authorization: String?, args: CreatePost): Result<PostResponse> =
    retrofitErrorHandler {
      api.createPost(generateHeaders(authorization, false), args.toCreatePost())
    }.map { it.toPostResponse() }

  override suspend fun editPost(authorization: String?, args: EditPost): Result<PostResponse> =
    retrofitErrorHandler {
      api.editPost(generateHeaders(authorization, false), args.toEditPost())
    }.map { it.toPostResponse() }

  override suspend fun deletePost(authorization: String?, args: DeletePost): Result<PostResponse> =
    retrofitErrorHandler { api.deletePost(generateHeaders(authorization, false), args) }
      .map { it.toPostResponse() }

  override suspend fun featurePost(
    authorization: String?,
    args: FeaturePost,
  ): Result<PostResponse> =
    retrofitErrorHandler { api.featurePost(generateHeaders(authorization, false), args) }
      .map { it.toPostResponse() }

  override suspend fun lockPost(authorization: String?, args: LockPost): Result<PostResponse> =
    retrofitErrorHandler { api.lockPost(generateHeaders(authorization, false), args) }
      .map { it.toPostResponse() }

  override suspend fun removePost(authorization: String?, args: RemovePost): Result<PostResponse> =
    retrofitErrorHandler { api.removePost(generateHeaders(authorization, false), args) }
      .map { it.toPostResponse() }

  override suspend fun search(
    authorization: String?,
    args: Search,
    force: Boolean,
  ): Result<SearchResponse> = retrofitErrorHandler {
    api.search(
      generateHeaders(authorization, force),
      args.serializeToMap(),
    )
  }
    .map {
      SearchResponse(
        type_ = it.type?.toSearchType() ?: SearchType.All,
        comments = listOf(),
        posts = it.posts.map { it.toPostView() },
        communities = it.communities.map { it.toCommunityView() },
        users = it.users.map { it.toPersonView() },
      )
    }

  override suspend fun getSiteMetadata(
    authorization: String?,
    args: GetSiteMetadata,
    force: Boolean,
  ): Result<GetSiteMetadataResponse> = retrofitErrorHandler {
    api.getSiteMetadata(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun createCommentReport(
    authorization: String?,
    args: CreateCommentReport,
  ): Result<CommentReportResponse> =
    retrofitErrorHandler { api.createCommentReport(generateHeaders(authorization, false), args) }
      .map {
        CommentReportResponse(
          comment_report_view = it.commentReportView.toCommentReportView(),
        )
      }

  override suspend fun createPostReport(
    authorization: String?,
    args: CreatePostReport,
  ): Result<PostReportResponse> =
    retrofitErrorHandler { api.createPostReport(generateHeaders(authorization, false), args) }
      .map {
        PostReportResponse(
          post_report_view = it.postReportView.toPostReportView(),
        )
      }

  override suspend fun blockPerson(
    authorization: String?,
    args: BlockPerson,
  ): Result<BlockPersonResponse> = retrofitErrorHandler {
    api.blockPerson(generateHeaders(authorization, false), args)
  }.map {
    BlockPersonResponse(
      person_view = it.personView.toPersonView(),
      blocked = it.blocked,
    )
  }

  override suspend fun blockCommunity(
    authorization: String?,
    args: BlockCommunity,
  ): Result<BlockCommunityResponse> = retrofitErrorHandler {
    api.blockCommunity(generateHeaders(authorization, false), args)
  }.map {
    BlockCommunityResponse(
      community_view = it.communityView.toCommunityView(),
      blocked = it.blocked,
    )
  }

  override suspend fun blockInstance(
    authorization: String?,
    args: BlockInstance,
  ): Result<BlockInstanceResponse> = retrofitErrorHandler {
    api.blockInstance(generateHeaders(authorization, false), args)
  }.map {
    BlockInstanceResponse(
      blocked = it.blocked,
    )
  }

  override suspend fun saveUserSettings(
    authorization: String?,
    args: SaveUserSettings,
  ): Result<LoginResponse> =
    retrofitErrorHandler { api.saveUserSettings(generateHeaders(authorization, false), args) }
      .map { it.toLoginResponse() }

  override suspend fun uploadImage(
    authorization: String?,
    url: String,
    fileName: String,
    imageIs: InputStream,
    mimeType: String?,
  ): Result<UploadImageResult> = retrofitErrorHandler {
    api.uploadImage(
      headers = generateHeaders(authorization = authorization, force = false),
      token = "jwt=$authorization",
      filePart = MultipartBody.Part.createFormData(
        name = "file",
        filename = fileName,
        body = imageIs.readBytes().toRequestBody(contentType = mimeType?.toMediaType()),
      ),
    )
  }.map {
    UploadImageResult(it.url)
  }

  override suspend fun resolveObject(
    authorization: String?,
    args: ResolveObject,
    force: Boolean,
  ): Result<ResolveObjectResponse> = retrofitErrorHandler {
    api.resolveObject(generateHeaders(authorization, force), args.serializeToMap())
  }.map {
    ResolveObjectResponse(
      comment = it.comment?.toCommentView(),
      post = it.post?.toPostView(),
      community = it.community?.toCommunityView(),
      person = it.person?.toPersonView(),
    )
  }

  override suspend fun banUserFromSite(
    authorization: String?,
    args: BanPerson,
  ): Result<BanPersonResponse> =
    retrofitErrorHandler { api.banUserFromSite(generateHeaders(authorization, false), args) }

  override suspend fun removeCommunity(
    authorization: String?,
    args: RemoveCommunity,
  ): Result<CommunityResponse> = retrofitErrorHandler {
    api.removeCommunity(
      headers = generateHeaders(authorization, false),
      form = args,
    )
  }.map { it.toCommunityResponse() }

  override suspend fun hideCommunity(
    authorization: String?,
    args: HideCommunity,
  ): Result<SuccessResponse> =
    retrofitErrorHandler { api.hideCommunity(generateHeaders(authorization, false), args) }

  override suspend fun purgePerson(
    authorization: String?,
    args: PurgePerson,
  ): Result<SuccessResponse> =
    retrofitErrorHandler { api.purgePerson(generateHeaders(authorization, false), args) }

  override suspend fun purgeCommunity(
    authorization: String?,
    args: PurgeCommunity,
  ): Result<SuccessResponse> =
    retrofitErrorHandler { api.purgeCommunity(generateHeaders(authorization, false), args) }

  override suspend fun purgePost(
    authorization: String?,
    args: PurgePost,
  ): Result<SuccessResponse> =
    retrofitErrorHandler { api.purgePost(generateHeaders(authorization, false), args) }

  override suspend fun purgeComment(
    authorization: String?,
    args: PurgeComment,
  ): Result<SuccessResponse> =
    retrofitErrorHandler { api.purgeComment(generateHeaders(authorization, false), args) }

  override suspend fun getRegistrationApplicationsCount(
    authorization: String?,
    args: GetUnreadRegistrationApplicationCount,
    force: Boolean,
  ): Result<GetUnreadRegistrationApplicationCountResponse> = retrofitErrorHandler {
    api.getRegistrationApplicationsCount(
      generateHeaders(authorization, force),
      args.serializeToMap(),
    )
  }

  override suspend fun listRegistrationApplications(
    authorization: String?,
    args: ListRegistrationApplications,
    force: Boolean,
  ): Result<ListRegistrationApplicationsResponse> = retrofitErrorHandler {
    api.listRegistrationApplications(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun approveRegistrationApplication(
    authorization: String?,
    args: ApproveRegistrationApplication,
  ): Result<RegistrationApplicationResponse> = retrofitErrorHandler {
    api.approveRegistrationApplication(generateHeaders(authorization, false), args)
  }

  override suspend fun getModLogs(
    authorization: String?,
    args: GetModlog,
    force: Boolean,
  ): Result<GetModlogResponse> = retrofitErrorHandler {
    api.getModLogs(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun register(args: Register): Result<LoginResponse> =
    retrofitErrorHandler { api.register(generateHeaders(null, false), args) }
      .map { it.toLoginResponse() }

  override suspend fun getCaptcha(): Result<GetCaptchaResponse> =
    retrofitErrorHandler { api.getCaptcha(generateHeaders(null, false)) }

  override suspend fun listMedia(
    authorization: String?,
    args: ListMedia,
    force: Boolean,
  ): Result<ListMediaResponse> = retrofitErrorHandler {
    api.listMedia(
      generateHeaders(authorization, force),
      args.serializeToMap(),
    )
  }
}
