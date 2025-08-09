package com.idunnololz.summit.api

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
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
import com.idunnololz.summit.api.dto.lemmy.DeleteImage
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
import com.idunnololz.summit.api.dto.lemmy.SuccessResponse
import java.io.InputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class LemmyApiV3Adapter(
  private val api: LemmyApiV3,
  override val instance: String,
) : LemmyLikeApi {

  companion object {
    private val gson = GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create()
  }

  override fun supportsFeature(apiFeature: ApiFeature): Boolean = when (apiFeature) {
    ApiFeature.Reports -> true
    ApiFeature.Register -> true
    ApiFeature.Downvoted -> true
    ApiFeature.UploadsList -> true
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
    api.getSite(
      generateHeaders(authorization, force),
      args.serializeToMap(),
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

  override suspend fun getPost(
    authorization: String?,
    args: GetPost,
    force: Boolean,
  ): Result<GetPostResponse> = retrofitErrorHandler {
    api.getPost(
      generateHeaders(authorization, force),
      args.serializeToMap(),
    )
  }

  override suspend fun login(args: Login): Result<LoginResponse> =
    retrofitErrorHandler {
      // Lemmy has a password limit of 60 characters.
      api.login(generateHeaders(null, false), args.copy(password = args.password.take(60)))
    }

  override suspend fun likePost(
    authorization: String?,
    args: CreatePostLike,
  ): Result<PostResponse> =
    retrofitErrorHandler { api.likePost(generateHeaders(authorization, false), args) }

  override suspend fun likeComment(
    authorization: String?,
    args: CreateCommentLike,
  ): Result<CommentResponse> =
    retrofitErrorHandler { api.likeComment(generateHeaders(authorization, false), args) }

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
  ): Result<CommentResponse> =
    retrofitErrorHandler { api.createComment(generateHeaders(authorization, false), args) }

  override suspend fun editComment(
    authorization: String?,
    args: EditComment,
  ): Result<CommentResponse> =
    retrofitErrorHandler { api.editComment(generateHeaders(authorization, false), args) }

  override suspend fun deleteComment(
    authorization: String?,
    args: DeleteComment,
  ): Result<CommentResponse> =
    retrofitErrorHandler { api.deleteComment(generateHeaders(authorization, false), args) }

  override suspend fun savePost(authorization: String?, args: SavePost): Result<PostResponse> =
    retrofitErrorHandler { api.savePost(generateHeaders(authorization, false), args) }

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

  override suspend fun getComments(
    authorization: String?,
    args: GetComments,
    force: Boolean,
  ): Result<GetCommentsResponse> = retrofitErrorHandler {
    api.getComments(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun distinguishComment(
    authorization: String?,
    args: DistinguishComment,
  ): Result<CommentResponse> =
    retrofitErrorHandler { api.distinguishComment(generateHeaders(authorization, false), args) }

  override suspend fun removeComment(
    authorization: String?,
    args: RemoveComment,
  ): Result<CommentResponse> =
    retrofitErrorHandler { api.removeComment(generateHeaders(authorization, false), args) }

  override suspend fun getCommunity(
    authorization: String?,
    args: GetCommunity,
    force: Boolean,
  ): Result<GetCommunityResponse> = retrofitErrorHandler {
    api.getCommunity(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun createCommunity(
    authorization: String?,
    args: CreateCommunity,
  ): Result<CommunityResponse> =
    retrofitErrorHandler { api.createCommunity(generateHeaders(authorization, false), args) }

  override suspend fun updateCommunity(
    authorization: String?,
    args: EditCommunity,
  ): Result<CommunityResponse> =
    retrofitErrorHandler { api.updateCommunity(generateHeaders(authorization, false), args) }

  override suspend fun deleteCommunity(
    authorization: String?,
    args: DeleteCommunity,
  ): Result<CommunityResponse> =
    retrofitErrorHandler { api.deleteCommunity(generateHeaders(authorization, false), args) }

  override suspend fun getCommunityList(
    authorization: String?,
    args: ListCommunities,
    force: Boolean,
  ): Result<ListCommunitiesResponse> = retrofitErrorHandler {
    api.getCommunityList(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun getPersonDetails(
    authorization: String?,
    args: GetPersonDetails,
    force: Boolean,
  ): Result<GetPersonDetailsResponse> = retrofitErrorHandler {
    api.getPersonDetails(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun changePassword(
    authorization: String?,
    args: ChangePassword,
  ): Result<LoginResponse> =
    retrofitErrorHandler { api.changePassword(generateHeaders(authorization, false), args) }

  override suspend fun getReplies(
    authorization: String?,
    args: GetReplies,
    force: Boolean,
  ): Result<GetRepliesResponse> = retrofitErrorHandler {
    api.getReplies(generateHeaders(authorization, force), args.serializeToMap())
  }

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
  }

  override suspend fun markAllAsRead(
    authorization: String?,
    args: MarkAllAsRead,
  ): Result<GetRepliesResponse> =
    retrofitErrorHandler { api.markAllAsRead(generateHeaders(authorization, false), args) }

  override suspend fun getPersonMentions(
    authorization: String?,
    args: GetPersonMentions,
    force: Boolean,
  ): Result<GetPersonMentionsResponse> = retrofitErrorHandler {
    api.getPersonMentions(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun getPrivateMessages(
    authorization: String?,
    args: GetPrivateMessages,
    force: Boolean,
  ): Result<PrivateMessagesResponse> = retrofitErrorHandler {
    api.getPrivateMessages(generateHeaders(authorization, force), args.serializeToMap())
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

  override suspend fun createPrivateMessage(
    authorization: String?,
    args: CreatePrivateMessage,
  ): Result<PrivateMessageResponse> =
    retrofitErrorHandler { api.createPrivateMessage(generateHeaders(authorization, false), args) }

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
  ): Result<CommunityResponse> =
    retrofitErrorHandler { api.followCommunity(generateHeaders(authorization, false), args) }

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

  override suspend fun createPost(authorization: String?, args: CreatePost): Result<PostResponse> =
    retrofitErrorHandler { api.createPost(generateHeaders(authorization, false), args) }

  override suspend fun editPost(authorization: String?, args: EditPost): Result<PostResponse> =
    retrofitErrorHandler { api.editPost(generateHeaders(authorization, false), args) }

  override suspend fun deletePost(authorization: String?, args: DeletePost): Result<PostResponse> =
    retrofitErrorHandler { api.deletePost(generateHeaders(authorization, false), args) }

  override suspend fun featurePost(
    authorization: String?,
    args: FeaturePost,
  ): Result<PostResponse> =
    retrofitErrorHandler { api.featurePost(generateHeaders(authorization, false), args) }

  override suspend fun lockPost(authorization: String?, args: LockPost): Result<PostResponse> =
    retrofitErrorHandler { api.lockPost(generateHeaders(authorization, false), args) }

  override suspend fun removePost(authorization: String?, args: RemovePost): Result<PostResponse> =
    retrofitErrorHandler { api.removePost(generateHeaders(authorization, false), args) }

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

  override suspend fun createPostReport(
    authorization: String?,
    args: CreatePostReport,
  ): Result<PostReportResponse> =
    retrofitErrorHandler { api.createPostReport(generateHeaders(authorization, false), args) }

  override suspend fun blockPerson(
    authorization: String?,
    args: BlockPerson,
  ): Result<BlockPersonResponse> =
    retrofitErrorHandler { api.blockPerson(generateHeaders(authorization, false), args) }

  override suspend fun blockCommunity(
    authorization: String?,
    args: BlockCommunity,
  ): Result<BlockCommunityResponse> =
    retrofitErrorHandler { api.blockCommunity(generateHeaders(authorization, false), args) }

  override suspend fun blockInstance(
    authorization: String?,
    args: BlockInstance,
  ): Result<BlockInstanceResponse> =
    retrofitErrorHandler { api.blockInstance(generateHeaders(authorization, false), args) }

  override suspend fun saveUserSettings(
    authorization: String?,
    args: SaveUserSettings,
  ): Result<LoginResponse> =
    retrofitErrorHandler { api.saveUserSettings(generateHeaders(authorization, false), args) }

  override suspend fun uploadImage(
    authorization: String?,
    url: String,
    fileName: String,
    imageIs: InputStream,
    mimeType: String?,
  ): Result<UploadImageResult> = retrofitErrorHandler {
    api.uploadImage(
      headers = generateHeaders(authorization, false),
      token = "jwt=$authorization",
      url = url,
      filePart = MultipartBody.Part.createFormData(
        name = "images[]",
        filename = fileName,
        body = imageIs.readBytes().toRequestBody(contentType = mimeType?.toMediaType()),
      ),
    )
  }.map {
    UploadImageResult("$url/${it.files?.get(0)?.file}")
  }

  override suspend fun resolveObject(
    authorization: String?,
    args: ResolveObject,
    force: Boolean,
  ): Result<ResolveObjectResponse> = retrofitErrorHandler {
    api.resolveObject(generateHeaders(authorization, force), args.serializeToMap())
  }

  override suspend fun banUserFromSite(
    authorization: String?,
    args: BanPerson,
  ): Result<BanPersonResponse> =
    retrofitErrorHandler { api.banUserFromSite(generateHeaders(authorization, false), args) }

  override suspend fun removeCommunity(
    authorization: String?,
    args: RemoveCommunity,
  ): Result<CommunityResponse> =
    retrofitErrorHandler { api.removeCommunity(generateHeaders(authorization, false), args) }

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

  override suspend fun deleteMedia(
    authorization: String?,
    args: DeleteImage,
  ): Result<Unit> = retrofitErrorHandler {
    // https://lemmy.world/pictrs/image/delete/b60f8360-38bd-450a-ad6c-27b0b3936a27/60ac57fb-0bdd-42af-899a-01982ad37285.jpeg

    api.deleteMedia(
      url = "https://${instance}/pictrs/image/delete/${args.delete_token}/${args.filename}",
      headers = generateHeaders(authorization, force = false),
    )
  }
}
