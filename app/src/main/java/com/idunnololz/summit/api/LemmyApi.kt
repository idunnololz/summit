package com.idunnololz.summit.api

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
import com.idunnololz.summit.api.dto.GetCommentsResponse
import com.idunnololz.summit.api.dto.GetCommunityResponse
import com.idunnololz.summit.api.dto.GetModlogResponse
import com.idunnololz.summit.api.dto.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.GetPersonMentionsResponse
import com.idunnololz.summit.api.dto.GetPostResponse
import com.idunnololz.summit.api.dto.GetPostsResponse
import com.idunnololz.summit.api.dto.GetRepliesResponse
import com.idunnololz.summit.api.dto.GetReportCountResponse
import com.idunnololz.summit.api.dto.GetSiteMetadataResponse
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.GetUnreadCountResponse
import com.idunnololz.summit.api.dto.GetUnreadRegistrationApplicationCountResponse
import com.idunnololz.summit.api.dto.HideCommunity
import com.idunnololz.summit.api.dto.ListCommentLikesResponse
import com.idunnololz.summit.api.dto.ListCommentReportsResponse
import com.idunnololz.summit.api.dto.ListCommunitiesResponse
import com.idunnololz.summit.api.dto.ListMediaResponse
import com.idunnololz.summit.api.dto.ListPostLikesResponse
import com.idunnololz.summit.api.dto.ListPostReportsResponse
import com.idunnololz.summit.api.dto.ListPrivateMessageReportsResponse
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
import com.idunnololz.summit.api.dto.ResolveObjectResponse
import com.idunnololz.summit.api.dto.ResolvePostReport
import com.idunnololz.summit.api.dto.ResolvePrivateMessageReport
import com.idunnololz.summit.api.dto.SaveComment
import com.idunnololz.summit.api.dto.SavePost
import com.idunnololz.summit.api.dto.SaveUserSettings
import com.idunnololz.summit.api.dto.SearchResponse
import com.idunnololz.summit.api.dto.SuccessResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.QueryMap
import retrofit2.http.Url

private const val TAG = "LemmyApi"

interface LemmyApi {
  @GET("site")
  fun getSite(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetSiteResponse>

  @GET("site")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getSiteNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetSiteResponse>

  /**
   * Get / fetch posts, with various filters.
   */
  @GET("post/list")
  fun getPosts(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetPostsResponse>

  @GET("post/list")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getPostsNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetPostsResponse>

  /**
   * Get / fetch a post.
   */
  @GET("post")
  fun getPost(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetPostResponse>

  @GET("post")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getPostNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetPostResponse>

  /**
   * Log into lemmy.
   */
  @POST("user/login")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun login(@Body form: Login): Call<LoginResponse>

  /**
   * Like / vote on a post.
   */
  @POST("post/like")
  fun likePost(
    @Header("Authorization") authorization: String?,
    @Body form: CreatePostLike,
  ): Call<PostResponse>

  /**
   * Like / vote on a comment.
   */
  @POST("comment/like")
  fun likeComment(
    @Header("Authorization") authorization: String?,
    @Body form: CreateCommentLike,
  ): Call<CommentResponse>

  @GET("comment/like/list")
  fun listCommentVotes(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListCommentLikesResponse>

  @GET("comment/like/list")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun listCommentVotesNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListCommentLikesResponse>

  @GET("post/like/list")
  fun listPostVotes(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListPostLikesResponse>

  @GET("post/like/list")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun listPostVotesNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListPostLikesResponse>

  /**
   * Create a comment.
   */
  @POST("comment")
  fun createComment(
    @Header("Authorization") authorization: String?,
    @Body form: CreateComment,
  ): Call<CommentResponse>

  /**
   * Edit a comment.
   */
  @PUT("comment")
  fun editComment(
    @Header("Authorization") authorization: String?,
    @Body form: EditComment,
  ): Call<CommentResponse>

  /**
   * Delete a comment.
   */
  @POST("comment/delete")
  fun deleteComment(
    @Header("Authorization") authorization: String?,
    @Body form: DeleteComment,
  ): Call<CommentResponse>

  /**
   * Save a post.
   */
  @PUT("post/save")
  fun savePost(
    @Header("Authorization") authorization: String?,
    @Body form: SavePost,
  ): Call<PostResponse>

  @POST("post/mark_as_read")
  fun markPostAsRead(
    @Header("Authorization") authorization: String?,
    @Body form: MarkPostAsRead,
  ): Call<PostResponse>

  /**
   * Save a comment.
   */
  @PUT("comment/save")
  fun saveComment(
    @Header("Authorization") authorization: String?,
    @Body form: SaveComment,
  ): Call<CommentResponse>

  /**
   * Get / fetch comments.
   */
  @GET("comment/list")
  fun getComments(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetCommentsResponse>

  @GET("comment/list")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getCommentsNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetCommentsResponse>

  @POST("comment/distinguish")
  fun distinguishComment(
    @Header("Authorization") authorization: String?,
    @Body form: DistinguishComment,
  ): Call<CommentResponse>

  @POST("comment/remove")
  fun removeComment(
    @Header("Authorization") authorization: String?,
    @Body form: RemoveComment,
  ): Call<CommentResponse>

  /**
   * Get / fetch a community.
   */
  @GET("community")
  fun getCommunity(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetCommunityResponse>

  @GET("community")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getCommunityNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetCommunityResponse>

  /**
   * Create a community.
   */
  @POST("community")
  fun createCommunity(
    @Header("Authorization") authorization: String?,
    @Body createCommunity: CreateCommunity,
  ): Call<CommunityResponse>

  /**
   * Update a community.
   */
  @PUT("community")
  fun updateCommunity(
    @Header("Authorization") authorization: String?,
    @Body editCommunity: EditCommunity,
  ): Call<CommunityResponse>

  /**
   * Delete a community.
   */
  @POST("community/delete")
  fun deleteCommunity(
    @Header("Authorization") authorization: String?,
    @Body deleteCommunity: DeleteCommunity,
  ): Call<CommunityResponse>

  /**
   * Get / fetch a community.
   */
  @GET("community/list")
  fun getCommunityList(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListCommunitiesResponse>

  /**
   * Get the details for a person.
   */
  @GET("user")
  fun getPersonDetails(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetPersonDetailsResponse>

  @GET("user")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getPersonDetailsNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetPersonDetailsResponse>

  @PUT("user/change_password")
  fun changePassword(
    @Header("Authorization") authorization: String?,
    @Body form: ChangePassword,
  ): Call<LoginResponse>

  /**
   * Get comment replies.
   */
  @GET("user/replies")
  fun getReplies(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetRepliesResponse>

  @GET("user/replies")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getRepliesNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetRepliesResponse>

  /**
   * Mark a comment as read.
   */
  @POST("comment/mark_as_read")
  fun markCommentReplyAsRead(
    @Header("Authorization") authorization: String?,
    @Body form: MarkCommentReplyAsRead,
  ): Call<CommentResponse>

  /**
   * Mark a person mention as read.
   */
  @POST("user/mention/mark_as_read")
  fun markPersonMentionAsRead(
    @Header("Authorization") authorization: String?,
    @Body form: MarkPersonMentionAsRead,
  ): Call<PersonMentionResponse>

  /**
   * Mark a private message as read.
   */
  @POST("private_message/mark_as_read")
  fun markPrivateMessageAsRead(
    @Header("Authorization") authorization: String?,
    @Body form: MarkPrivateMessageAsRead,
  ): Call<PrivateMessageResponse>

  /**
   * Mark all replies as read.
   */
  @POST("user/mark_all_as_read")
  fun markAllAsRead(
    @Header("Authorization") authorization: String?,
    @Body form: MarkAllAsRead,
  ): Call<GetRepliesResponse>

  /**
   * Get mentions for your user.
   */
  @GET("user/mention")
  fun getPersonMentions(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetPersonMentionsResponse>

  @GET("user/mention")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getPersonMentionsNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetPersonMentionsResponse>

  /**
   * Get / fetch private messages.
   */
  @GET("private_message/list")
  fun getPrivateMessages(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<PrivateMessagesResponse>

  @GET("private_message/list")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getPrivateMessagesNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<PrivateMessagesResponse>

  /**
   * These are instance wide reports that are only visible for instance admins.
   */
  @GET("private_message/report/list")
  fun getPrivateMessageReports(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListPrivateMessageReportsResponse>

  @GET("private_message/report/list")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getPrivateMessageReportsNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListPrivateMessageReportsResponse>

  @POST("private_message/report")
  fun createPrivateMessageReport(
    @Header("Authorization") authorization: String?,
    @Body form: CreatePrivateMessageReport,
  ): Call<PrivateMessageReportResponse>

  @PUT("private_message/report/resolve")
  fun resolvePrivateMessageReport(
    @Header("Authorization") authorization: String?,
    @Body resolvePrivateMessageReport: ResolvePrivateMessageReport,
  ): Call<PrivateMessageReportResponse>

  @GET("post/report/list")
  fun getPostReports(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListPostReportsResponse>

  @GET("post/report/list")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getPostReportsNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListPostReportsResponse>

  @PUT("post/report/resolve")
  fun resolvePostReport(
    @Header("Authorization") authorization: String?,
    @Body resolvePostReport: ResolvePostReport,
  ): Call<PostReportResponse>

  @GET("comment/report/list")
  fun getCommentReports(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListCommentReportsResponse>

  @GET("comment/report/list")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getCommentReportsNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListCommentReportsResponse>

  @PUT("comment/report/resolve")
  fun resolveCommentReport(
    @Header("Authorization") authorization: String?,
    @Body resolveCommentReport: ResolveCommentReport,
  ): Call<CommentReportResponse>

  /**
   * Create a private message.
   */
  @POST("private_message")
  fun createPrivateMessage(
    @Header("Authorization") authorization: String?,
    @Body form: CreatePrivateMessage,
  ): Call<PrivateMessageResponse>

  /**
   * Get your unread counts
   */
  @GET("user/unread_count")
  fun getUnreadCount(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetUnreadCountResponse>

  @GET("user/unread_count")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getUnreadCountNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetUnreadCountResponse>

  @GET("user/report_count")
  fun getReportCount(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetReportCountResponse>

  @GET("user/report_count")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getReportCountNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetReportCountResponse>

  /**
   * Follow / subscribe to a community.
   */
  @POST("community/follow")
  fun followCommunity(
    @Header("Authorization") authorization: String?,
    @Body form: FollowCommunity,
  ): Call<CommunityResponse>

  @POST("community/ban_user")
  fun banUserFromCommunity(
    @Header("Authorization") authorization: String?,
    @Body banUser: BanFromCommunity,
  ): Call<BanFromCommunityResponse>

  @POST("community/mod")
  fun modUser(
    @Header("Authorization") authorization: String?,
    @Body modUser: AddModToCommunity,
  ): Call<AddModToCommunityResponse>

  /**
   * Create a post.
   */
  @POST("post")
  fun createPost(
    @Header("Authorization") authorization: String?,
    @Body form: CreatePost,
  ): Call<PostResponse>

  /**
   * Edit a post.
   */
  @PUT("post")
  fun editPost(
    @Header("Authorization") authorization: String?,
    @Body form: EditPost,
  ): Call<PostResponse>

  /**
   * Delete a post.
   */
  @POST("post/delete")
  fun deletePost(
    @Header("Authorization") authorization: String?,
    @Body form: DeletePost,
  ): Call<PostResponse>

  @POST("post/feature")
  fun featurePost(
    @Header("Authorization") authorization: String?,
    @Body form: FeaturePost,
  ): Call<PostResponse>

  @POST("post/lock")
  fun lockPost(
    @Header("Authorization") authorization: String?,
    @Body form: LockPost,
  ): Call<PostResponse>

  @POST("post/remove")
  fun removePost(
    @Header("Authorization") authorization: String?,
    @Body form: RemovePost,
  ): Call<PostResponse>

  /**
   * Search lemmy.
   */
  @GET("search")
  fun search(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<SearchResponse>

  @GET("search")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun searchNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<SearchResponse>

  /**
   * Fetch metadata for any given site.
   */
  @GET("post/site_metadata")
  fun getSiteMetadata(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetSiteMetadataResponse>

  /**
   * Report a comment.
   */
  @POST("comment/report")
  fun createCommentReport(
    @Header("Authorization") authorization: String?,
    @Body form: CreateCommentReport,
  ): Call<CommentReportResponse>

  /**
   * Report a post.
   */
  @POST("post/report")
  fun createPostReport(
    @Header("Authorization") authorization: String?,
    @Body form: CreatePostReport,
  ): Call<PostReportResponse>

  /**
   * Block a person.
   */
  @POST("user/block")
  fun blockPerson(
    @Header("Authorization") authorization: String?,
    @Body form: BlockPerson,
  ): Call<BlockPersonResponse>

  /**
   * Block a community.
   */
  @POST("community/block")
  fun blockCommunity(
    @Header("Authorization") authorization: String?,
    @Body form: BlockCommunity,
  ): Call<BlockCommunityResponse>

  /**
   * Block an instance.
   */
  @POST("site/block")
  fun blockInstance(
    @Header("Authorization") authorization: String?,
    @Body form: BlockInstance,
  ): Call<BlockInstanceResponse>

  /**
   * Save your user settings.
   */
  @PUT("user/save_user_settings")
  fun saveUserSettings(
    @Header("Authorization") authorization: String?,
    @Body form: SaveUserSettings,
  ): Call<LoginResponse>

  /**
   * Upload an image.
   */
  @Multipart
  @POST
  fun uploadImage(
    @Header("Authorization") authorization: String?,
    @Header("Cookie") token: String,
    @Url url: String,
    @Part filePart: MultipartBody.Part,
  ): Call<PictrsImages>

  @GET("resolve_object")
  fun resolveObject(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ResolveObjectResponse>

  /**
   * Admin endpoints
   */

  @POST("user/ban")
  fun banUserFromSite(
    @Header("Authorization") authorization: String?,
    @Body form: BanPerson,
  ): Call<BanPersonResponse>

  @POST("community/remove")
  fun removeCommunity(
    @Header("Authorization") authorization: String?,
    @Body form: RemoveCommunity,
  ): Call<CommunityResponse>

  @PUT("community/hide")
  fun hideCommunity(
    @Header("Authorization") authorization: String?,
    @Body form: HideCommunity,
  ): Call<SuccessResponse>

  /**
   * Admin endpoints
   */

  @POST("admin/purge/person")
  fun purgePerson(
    @Header("Authorization") authorization: String?,
    @Body form: PurgePerson,
  ): Call<SuccessResponse>

  @POST("admin/purge/community")
  fun purgeCommunity(
    @Header("Authorization") authorization: String?,
    @Body form: PurgeCommunity,
  ): Call<SuccessResponse>

  @POST("admin/purge/post")
  fun purgePost(
    @Header("Authorization") authorization: String?,
    @Body form: PurgePost,
  ): Call<SuccessResponse>

  @POST("admin/purge/comment")
  fun purgeComment(
    @Header("Authorization") authorization: String?,
    @Body form: PurgeComment,
  ): Call<SuccessResponse>

  @GET("admin/registration_application/count")
  fun getRegistrationApplicationsCount(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetUnreadRegistrationApplicationCountResponse>

  @GET("admin/registration_application/count")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getRegistrationApplicationsCountNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetUnreadRegistrationApplicationCountResponse>

  @GET("admin/registration_application/list")
  fun listRegistrationApplications(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListRegistrationApplicationsResponse>

  @GET("admin/registration_application/list")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun listRegistrationApplicationsNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListRegistrationApplicationsResponse>

  @PUT("admin/registration_application/approve")
  fun approveRegistrationApplication(
    @Header("Authorization") authorization: String?,
    @Body form: ApproveRegistrationApplication,
  ): Call<RegistrationApplicationResponse>

  @GET("modlog")
  fun getModLogs(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetModlogResponse>

  @GET("modlog")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun getModLogsNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<GetModlogResponse>

  @POST("user/register")
  fun register(@Body register: Register): Call<LoginResponse>

  @GET("user/get_captcha")
  fun getCaptcha(): Call<GetCaptchaResponse>

  @GET("account/list_media")
  fun listMedia(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListMediaResponse>

  @GET("account/list_media")
  @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
  fun listMediaNoCache(
    @Header("Authorization") authorization: String?,
    @QueryMap form: Map<String, String>,
  ): Call<ListMediaResponse>

  // https://lemmy.world/pictrs/image/delete/b60f8360-38bd-450a-ad6c-27b0b3936a27/60ac57fb-0bdd-42af-899a-01982ad37285.jpeg

  companion object {

    internal const val CACHE_CONTROL_HEADER = "Cache-Control"
    internal const val CACHE_CONTROL_NO_CACHE = "no-cache"
  }
}

class LemmyApiWithSite(
  private val lemmyApi: LemmyApi,
  val instance: String,
) : LemmyApi by lemmyApi
