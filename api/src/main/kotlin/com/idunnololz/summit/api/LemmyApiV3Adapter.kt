package com.idunnololz.summit.api

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.idunnololz.summit.api.dto.AddModToCommunity
import com.idunnololz.summit.api.dto.AddModToCommunityResponse
import com.idunnololz.summit.api.dto.ApproveRegistrationApplication
import com.idunnololz.summit.api.dto.BanFromCommunity
import com.idunnololz.summit.api.dto.BanFromCommunityResponse
import com.idunnololz.summit.api.dto.BanPerson
import com.idunnololz.summit.api.dto.BanPersonResponse
import com.idunnololz.summit.api.dto.BlockCommunity
import com.idunnololz.summit.api.dto.BlockCommunityResponse
import com.idunnololz.summit.api.dto.BlockInstance
import com.idunnololz.summit.api.dto.BlockInstanceResponse
import com.idunnololz.summit.api.dto.BlockPerson
import com.idunnololz.summit.api.dto.BlockPersonResponse
import com.idunnololz.summit.api.dto.ChangePassword
import com.idunnololz.summit.api.dto.CommentReportResponse
import com.idunnololz.summit.api.dto.CommentResponse
import com.idunnololz.summit.api.dto.CommunityResponse
import com.idunnololz.summit.api.dto.CreateComment
import com.idunnololz.summit.api.dto.CreateCommentLike
import com.idunnololz.summit.api.dto.CreateCommentReport
import com.idunnololz.summit.api.dto.CreateCommunity
import com.idunnololz.summit.api.dto.CreatePost
import com.idunnololz.summit.api.dto.CreatePostLike
import com.idunnololz.summit.api.dto.CreatePostReport
import com.idunnololz.summit.api.dto.CreatePrivateMessage
import com.idunnololz.summit.api.dto.CreatePrivateMessageReport
import com.idunnololz.summit.api.dto.DeleteComment
import com.idunnololz.summit.api.dto.DeleteCommunity
import com.idunnololz.summit.api.dto.DeletePost
import com.idunnololz.summit.api.dto.DistinguishComment
import com.idunnololz.summit.api.dto.EditComment
import com.idunnololz.summit.api.dto.EditCommunity
import com.idunnololz.summit.api.dto.EditPost
import com.idunnololz.summit.api.dto.FeaturePost
import com.idunnololz.summit.api.dto.FollowCommunity
import com.idunnololz.summit.api.dto.GetCaptchaResponse
import com.idunnololz.summit.api.dto.GetComments
import com.idunnololz.summit.api.dto.GetCommentsResponse
import com.idunnololz.summit.api.dto.GetCommunity
import com.idunnololz.summit.api.dto.GetCommunityResponse
import com.idunnololz.summit.api.dto.GetModlog
import com.idunnololz.summit.api.dto.GetModlogResponse
import com.idunnololz.summit.api.dto.GetPersonDetails
import com.idunnololz.summit.api.dto.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.GetPersonMentions
import com.idunnololz.summit.api.dto.GetPersonMentionsResponse
import com.idunnololz.summit.api.dto.GetPost
import com.idunnololz.summit.api.dto.GetPostResponse
import com.idunnololz.summit.api.dto.GetPosts
import com.idunnololz.summit.api.dto.GetPostsResponse
import com.idunnololz.summit.api.dto.GetPrivateMessages
import com.idunnololz.summit.api.dto.GetReplies
import com.idunnololz.summit.api.dto.GetRepliesResponse
import com.idunnololz.summit.api.dto.GetReportCount
import com.idunnololz.summit.api.dto.GetReportCountResponse
import com.idunnololz.summit.api.dto.GetSite
import com.idunnololz.summit.api.dto.GetSiteMetadata
import com.idunnololz.summit.api.dto.GetSiteMetadataResponse
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.GetUnreadCount
import com.idunnololz.summit.api.dto.GetUnreadCountResponse
import com.idunnololz.summit.api.dto.GetUnreadRegistrationApplicationCount
import com.idunnololz.summit.api.dto.GetUnreadRegistrationApplicationCountResponse
import com.idunnololz.summit.api.dto.HideCommunity
import com.idunnololz.summit.api.dto.ListCommentLikes
import com.idunnololz.summit.api.dto.ListCommentLikesResponse
import com.idunnololz.summit.api.dto.ListCommentReports
import com.idunnololz.summit.api.dto.ListCommentReportsResponse
import com.idunnololz.summit.api.dto.ListCommunities
import com.idunnololz.summit.api.dto.ListCommunitiesResponse
import com.idunnololz.summit.api.dto.ListMedia
import com.idunnololz.summit.api.dto.ListMediaResponse
import com.idunnololz.summit.api.dto.ListPostLikes
import com.idunnololz.summit.api.dto.ListPostLikesResponse
import com.idunnololz.summit.api.dto.ListPostReports
import com.idunnololz.summit.api.dto.ListPostReportsResponse
import com.idunnololz.summit.api.dto.ListPrivateMessageReports
import com.idunnololz.summit.api.dto.ListPrivateMessageReportsResponse
import com.idunnololz.summit.api.dto.ListRegistrationApplications
import com.idunnololz.summit.api.dto.ListRegistrationApplicationsResponse
import com.idunnololz.summit.api.dto.LockPost
import com.idunnololz.summit.api.dto.Login
import com.idunnololz.summit.api.dto.LoginResponse
import com.idunnololz.summit.api.dto.MarkAllAsRead
import com.idunnololz.summit.api.dto.MarkCommentReplyAsRead
import com.idunnololz.summit.api.dto.MarkPersonMentionAsRead
import com.idunnololz.summit.api.dto.MarkPostAsRead
import com.idunnololz.summit.api.dto.MarkPrivateMessageAsRead
import com.idunnololz.summit.api.dto.PersonMentionResponse
import com.idunnololz.summit.api.dto.PictrsImages
import com.idunnololz.summit.api.dto.PostReportResponse
import com.idunnololz.summit.api.dto.PostResponse
import com.idunnololz.summit.api.dto.PrivateMessageReportResponse
import com.idunnololz.summit.api.dto.PrivateMessageResponse
import com.idunnololz.summit.api.dto.PrivateMessagesResponse
import com.idunnololz.summit.api.dto.PurgeComment
import com.idunnololz.summit.api.dto.PurgeCommunity
import com.idunnololz.summit.api.dto.PurgePerson
import com.idunnololz.summit.api.dto.PurgePost
import com.idunnololz.summit.api.dto.Register
import com.idunnololz.summit.api.dto.RegistrationApplicationResponse
import com.idunnololz.summit.api.dto.RemoveComment
import com.idunnololz.summit.api.dto.RemoveCommunity
import com.idunnololz.summit.api.dto.RemovePost
import com.idunnololz.summit.api.dto.ResolveCommentReport
import com.idunnololz.summit.api.dto.ResolveObject
import com.idunnololz.summit.api.dto.ResolveObjectResponse
import com.idunnololz.summit.api.dto.ResolvePostReport
import com.idunnololz.summit.api.dto.ResolvePrivateMessageReport
import com.idunnololz.summit.api.dto.SaveComment
import com.idunnololz.summit.api.dto.SavePost
import com.idunnololz.summit.api.dto.SaveUserSettings
import com.idunnololz.summit.api.dto.Search
import com.idunnololz.summit.api.dto.SearchResponse
import com.idunnololz.summit.api.dto.SuccessResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import java.io.InputStream

class LemmyApiV3Adapter(
  private val api: LemmyApiV3,
  override val instance: String,
) : LemmyLikeApi {

  companion object {
    private val gson = GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create()
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

  override fun getSite(
    authorization: String?,
    args: GetSite,
    force: Boolean,
  ): Call<GetSiteResponse> {
    return api.getSite(generateHeaders(authorization, force), args.serializeToMap())
  }

  override fun getPosts(
    authorization: String?,
    args: GetPosts,
    force: Boolean,
  ): Call<GetPostsResponse> =
    api.getPosts(generateHeaders(authorization, force), args.serializeToMap())

  override fun getPost(
    authorization: String?,
    args: GetPost,
    force: Boolean,
  ): Call<GetPostResponse> =
    api.getPost(generateHeaders(authorization, force), args.serializeToMap())

  override fun login(args: Login): Call<LoginResponse> =
    api.login(generateHeaders(null, false), args)

  override fun likePost(
    authorization: String?,
    args: CreatePostLike,
  ): Call<PostResponse> =
    api.likePost(generateHeaders(authorization, false), args)

  override fun likeComment(
    authorization: String?,
    args: CreateCommentLike,
  ): Call<CommentResponse> =
    api.likeComment(generateHeaders(authorization, false), args)

  override fun listCommentVotes(
    authorization: String?,
    args: ListCommentLikes,
    force: Boolean,
  ): Call<ListCommentLikesResponse> =
    api.listCommentVotes(generateHeaders(authorization, force), args.serializeToMap())

  override fun listPostVotes(
    authorization: String?,
    args: ListPostLikes,
    force: Boolean,
  ): Call<ListPostLikesResponse> =
    api.listPostVotes(generateHeaders(authorization, force), args.serializeToMap())

  override fun createComment(
    authorization: String?,
    args: CreateComment,
  ): Call<CommentResponse> =
    api.createComment(generateHeaders(authorization, false), args)

  override fun editComment(
    authorization: String?,
    args: EditComment,
  ): Call<CommentResponse> =
    api.editComment(generateHeaders(authorization, false), args)

  override fun deleteComment(
    authorization: String?,
    args: DeleteComment
  ): Call<CommentResponse> =
    api.deleteComment(generateHeaders(authorization, false), args)

  override fun savePost(
    authorization: String?,
    args: SavePost,
  ): Call<PostResponse> =
    api.savePost(generateHeaders(authorization, false), args)

  override fun markPostAsRead(
    authorization: String?,
    args: MarkPostAsRead,
  ): Call<PostResponse> =
    api.markPostAsRead(generateHeaders(authorization, false), args)

  override fun saveComment(
    authorization: String?,
    args: SaveComment,
  ): Call<CommentResponse> =
    api.saveComment(generateHeaders(authorization, false), args)

  override fun getComments(
    authorization: String?,
    args: GetComments,
    force: Boolean,
  ): Call<GetCommentsResponse> =
    api.getComments(generateHeaders(authorization, force), args.serializeToMap())

  override fun distinguishComment(
    authorization: String?,
    args: DistinguishComment,
  ): Call<CommentResponse> =
    api.distinguishComment(generateHeaders(authorization, false), args)

  override fun removeComment(
    authorization: String?,
    args: RemoveComment,
  ): Call<CommentResponse> =
    api.removeComment(generateHeaders(authorization, false), args)

  override fun getCommunity(
    authorization: String?,
    args: GetCommunity,
    force: Boolean,
  ): Call<GetCommunityResponse> =
    api.getCommunity(generateHeaders(authorization, force), args.serializeToMap())

  override fun createCommunity(
    authorization: String?,
    args: CreateCommunity,
  ): Call<CommunityResponse> =
    api.createCommunity(generateHeaders(authorization, false), args)

  override fun updateCommunity(
    authorization: String?,
    args: EditCommunity,
  ): Call<CommunityResponse> =
    api.updateCommunity(generateHeaders(authorization, false), args)

  override fun deleteCommunity(
    authorization: String?,
    args: DeleteCommunity,
  ): Call<CommunityResponse> =
    api.deleteCommunity(generateHeaders(authorization, false), args)

  override fun getCommunityList(
    authorization: String?,
    args: ListCommunities,
    force: Boolean,
  ): Call<ListCommunitiesResponse> =
    api.getCommunityList(generateHeaders(authorization, force), args.serializeToMap())

  override fun getPersonDetails(
    authorization: String?,
    args: GetPersonDetails,
    force: Boolean,
  ): Call<GetPersonDetailsResponse> =
    api.getPersonDetails(generateHeaders(authorization, force), args.serializeToMap())

  override fun changePassword(
    authorization: String?,
    args: ChangePassword,
  ): Call<LoginResponse> =
    api.changePassword(generateHeaders(authorization, false), args)

  override fun getReplies(
    authorization: String?,
    args: GetReplies,
    force: Boolean,
  ): Call<GetRepliesResponse> =
    api.getReplies(generateHeaders(authorization, force), args.serializeToMap())

  override fun markCommentReplyAsRead(
    authorization: String?,
    args: MarkCommentReplyAsRead,
  ): Call<CommentResponse> =
    api.markCommentReplyAsRead(generateHeaders(authorization, false), args)

  override fun markPersonMentionAsRead(
    authorization: String?,
    args: MarkPersonMentionAsRead,
  ): Call<PersonMentionResponse> =
    api.markPersonMentionAsRead(generateHeaders(authorization, false), args)

  override fun markPrivateMessageAsRead(
    authorization: String?,
    args: MarkPrivateMessageAsRead,
  ): Call<PrivateMessageResponse> =
    api.markPrivateMessageAsRead(generateHeaders(authorization, false), args)

  override fun markAllAsRead(
    authorization: String?,
    args: MarkAllAsRead,
  ): Call<GetRepliesResponse> =
    api.markAllAsRead(generateHeaders(authorization, false), args)

  override fun getPersonMentions(
    authorization: String?,
    args: GetPersonMentions,
    force: Boolean,
  ): Call<GetPersonMentionsResponse> =
    api.getPersonMentions(generateHeaders(authorization, force), args.serializeToMap())

  override fun getPrivateMessages(
    authorization: String?,
    args: GetPrivateMessages,
    force: Boolean,
  ): Call<PrivateMessagesResponse> =
    api.getPrivateMessages(generateHeaders(authorization, force), args.serializeToMap())

  override fun getPrivateMessageReports(
    authorization: String?,
    args: ListPrivateMessageReports,
    force: Boolean,
  ): Call<ListPrivateMessageReportsResponse> =
    api.getPrivateMessageReports(generateHeaders(authorization, force), args.serializeToMap())

  override fun createPrivateMessageReport(
    authorization: String?,
    args: CreatePrivateMessageReport,
  ): Call<PrivateMessageReportResponse> =
    api.createPrivateMessageReport(generateHeaders(authorization, false), args)

  override fun resolvePrivateMessageReport(
    authorization: String?,
    args: ResolvePrivateMessageReport,
  ): Call<PrivateMessageReportResponse> =
    api.resolvePrivateMessageReport(generateHeaders(authorization, false), args)

  override fun getPostReports(
    authorization: String?,
    args: ListPostReports,
    force: Boolean,
  ): Call<ListPostReportsResponse> =
    api.getPostReports(generateHeaders(authorization, force), args.serializeToMap())

  override fun resolvePostReport(
    authorization: String?,
    args: ResolvePostReport,
  ): Call<PostReportResponse> =
    api.resolvePostReport(generateHeaders(authorization, false), args)

  override fun getCommentReports(
    authorization: String?,
    args: ListCommentReports,
    force: Boolean,
  ): Call<ListCommentReportsResponse> =
    api.getCommentReports(generateHeaders(authorization, force), args.serializeToMap())

  override fun resolveCommentReport(
    authorization: String?,
    args: ResolveCommentReport,
  ): Call<CommentReportResponse> =
    api.resolveCommentReport(generateHeaders(authorization, false), args)

  override fun createPrivateMessage(
    authorization: String?,
    args: CreatePrivateMessage,
  ): Call<PrivateMessageResponse> =
    api.createPrivateMessage(generateHeaders(authorization, false), args)

  override fun getUnreadCount(
    authorization: String?,
    args: GetUnreadCount,
    force: Boolean,
  ): Call<GetUnreadCountResponse> =
    api.getUnreadCount(generateHeaders(authorization, force), args.serializeToMap())

  override fun getReportCount(
    authorization: String?,
    args: GetReportCount,
    force: Boolean,
  ): Call<GetReportCountResponse> =
    api.getReportCount(generateHeaders(authorization, force), args.serializeToMap())

  override fun followCommunity(
    authorization: String?,
    args: FollowCommunity,
  ): Call<CommunityResponse> =
    api.followCommunity(generateHeaders(authorization, false), args)

  override fun banUserFromCommunity(
    authorization: String?,
    args: BanFromCommunity,
  ): Call<BanFromCommunityResponse> =
    api.banUserFromCommunity(generateHeaders(authorization, false), args)

  override fun modUser(
    authorization: String?,
    args: AddModToCommunity,
  ): Call<AddModToCommunityResponse> =
    api.modUser(generateHeaders(authorization, false), args)

  override fun createPost(
    authorization: String?,
    args: CreatePost,
  ): Call<PostResponse> =
    api.createPost(generateHeaders(authorization, false), args)

  override fun editPost(
    authorization: String?,
    args: EditPost,
  ): Call<PostResponse> =
    api.editPost(generateHeaders(authorization, false), args)

  override fun deletePost(
    authorization: String?,
    args: DeletePost,
  ): Call<PostResponse> =
    api.deletePost(generateHeaders(authorization, false), args)

  override fun featurePost(
    authorization: String?,
    args: FeaturePost,
  ): Call<PostResponse> =
    api.featurePost(generateHeaders(authorization, false), args)

  override fun lockPost(
    authorization: String?,
    args: LockPost,
  ): Call<PostResponse> =
    api.lockPost(generateHeaders(authorization, false), args)

  override fun removePost(
    authorization: String?,
    args: RemovePost,
  ): Call<PostResponse> =
    api.removePost(generateHeaders(authorization, false), args)

  override fun search(
    authorization: String?,
    args: Search,
    force: Boolean,
  ): Call<SearchResponse> =
    api.search(generateHeaders(authorization, force), args.serializeToMap())

  override fun getSiteMetadata(
    authorization: String?,
    args: GetSiteMetadata,
    force: Boolean,
  ): Call<GetSiteMetadataResponse> =
    api.getSiteMetadata(generateHeaders(authorization, force), args.serializeToMap())

  override fun createCommentReport(
    authorization: String?,
    args: CreateCommentReport,
  ): Call<CommentReportResponse> =
    api.createCommentReport(generateHeaders(authorization, false), args)

  override fun createPostReport(
    authorization: String?,
    args: CreatePostReport,
  ): Call<PostReportResponse> =
    api.createPostReport(generateHeaders(authorization, false), args)

  override fun blockPerson(
    authorization: String?,
    args: BlockPerson,
  ): Call<BlockPersonResponse> =
    api.blockPerson(generateHeaders(authorization, false), args)

  override fun blockCommunity(
    authorization: String?,
    args: BlockCommunity,
  ): Call<BlockCommunityResponse> =
    api.blockCommunity(generateHeaders(authorization, false), args)

  override fun blockInstance(
    authorization: String?,
    args: BlockInstance,
  ): Call<BlockInstanceResponse> =
    api.blockInstance(generateHeaders(authorization, false), args)

  override fun saveUserSettings(
    authorization: String?,
    args: SaveUserSettings,
  ): Call<LoginResponse> =
    api.saveUserSettings(generateHeaders(authorization, false), args)

  override fun uploadImage(
    authorization: String?,
    url: String,
    fileName: String,
    imageIs: InputStream,
  ): Call<PictrsImages> =
    api.uploadImage(generateHeaders(authorization, false), "jwt=${authorization}", url, MultipartBody.Part.createFormData(
      "images[]",
      fileName,
      imageIs.readBytes().toRequestBody(),
    ))

  override fun resolveObject(
    authorization: String?,
    args: ResolveObject,
    force: Boolean,
  ): Call<ResolveObjectResponse> =
    api.resolveObject(generateHeaders(authorization, force), args.serializeToMap())

  override fun banUserFromSite(
    authorization: String?,
    args: BanPerson,
  ): Call<BanPersonResponse> =
    api.banUserFromSite(generateHeaders(authorization, false), args)

  override fun removeCommunity(
    authorization: String?,
    args: RemoveCommunity,
  ): Call<CommunityResponse> =
    api.removeCommunity(generateHeaders(authorization, false), args)

  override fun hideCommunity(
    authorization: String?,
    args: HideCommunity,
  ): Call<SuccessResponse> =
    api.hideCommunity(generateHeaders(authorization, false), args)

  override fun purgePerson(
    authorization: String?,
    args: PurgePerson,
  ): Call<SuccessResponse> =
    api.purgePerson(generateHeaders(authorization, false), args)

  override fun purgeCommunity(
    authorization: String?,
    args: PurgeCommunity,
  ): Call<SuccessResponse> =
    api.purgeCommunity(generateHeaders(authorization, false), args)

  override fun purgePost(
    authorization: String?,
    args: PurgePost,
  ): Call<SuccessResponse> =
    api.purgePost(generateHeaders(authorization, false), args)

  override fun purgeComment(
    authorization: String?,
    args: PurgeComment,
  ): Call<SuccessResponse> =
    api.purgeComment(generateHeaders(authorization, false), args)

  override fun getRegistrationApplicationsCount(
    authorization: String?,
    args: GetUnreadRegistrationApplicationCount,
    force: Boolean,
  ): Call<GetUnreadRegistrationApplicationCountResponse> =
    api.getRegistrationApplicationsCount(generateHeaders(authorization, force), args.serializeToMap())

  override fun listRegistrationApplications(
    authorization: String?,
    args: ListRegistrationApplications,
    force: Boolean,
  ): Call<ListRegistrationApplicationsResponse> =
    api.listRegistrationApplications(generateHeaders(authorization, force), args.serializeToMap())

  override fun approveRegistrationApplication(
    authorization: String?,
    args: ApproveRegistrationApplication,
  ): Call<RegistrationApplicationResponse> =
    api.approveRegistrationApplication(generateHeaders(authorization, false), args)

  override fun getModLogs(
    authorization: String?,
    args: GetModlog,
    force: Boolean,
  ): Call<GetModlogResponse> =
    api.getModLogs(generateHeaders(authorization, force), args.serializeToMap())

  override fun register(args: Register): Call<LoginResponse> =
    api.register(generateHeaders(null, false), args)

  override fun getCaptcha(): Call<GetCaptchaResponse> =
    api.getCaptcha(generateHeaders(null, false))

  override fun listMedia(
    authorization: String?,
    args: ListMedia,
    force: Boolean,
  ): Call<ListMediaResponse> =
    api.listMedia(generateHeaders(authorization, force), args.serializeToMap())


}