package com.idunnololz.summit.api

import com.idunnololz.summit.api.dto.lemmy.AddModToCommunity
import com.idunnololz.summit.api.dto.lemmy.BanFromCommunity
import com.idunnololz.summit.api.dto.lemmy.BanFromCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.BanPerson
import com.idunnololz.summit.api.dto.lemmy.BanPersonResponse
import com.idunnololz.summit.api.dto.lemmy.BlockCommunity
import com.idunnololz.summit.api.dto.lemmy.BlockInstance
import com.idunnololz.summit.api.dto.lemmy.BlockPerson
import com.idunnololz.summit.api.dto.lemmy.CreateCommentLike
import com.idunnololz.summit.api.dto.lemmy.CreateCommentReport
import com.idunnololz.summit.api.dto.lemmy.CreateCommunity
import com.idunnololz.summit.api.dto.lemmy.CreatePostLike
import com.idunnololz.summit.api.dto.lemmy.CreatePostReport
import com.idunnololz.summit.api.dto.lemmy.CreatePrivateMessage
import com.idunnololz.summit.api.dto.lemmy.CreatePrivateMessageReport
import com.idunnololz.summit.api.dto.lemmy.DeleteComment
import com.idunnololz.summit.api.dto.lemmy.DeleteCommunity
import com.idunnololz.summit.api.dto.lemmy.DeletePost
import com.idunnololz.summit.api.dto.lemmy.DistinguishComment
import com.idunnololz.summit.api.dto.lemmy.EditCommunity
import com.idunnololz.summit.api.dto.lemmy.FeaturePost
import com.idunnololz.summit.api.dto.lemmy.FollowCommunity
import com.idunnololz.summit.api.dto.lemmy.GetCaptchaResponse
import com.idunnololz.summit.api.dto.lemmy.GetReportCountResponse
import com.idunnololz.summit.api.dto.lemmy.GetSiteMetadataResponse
import com.idunnololz.summit.api.dto.lemmy.GetUnreadCountResponse
import com.idunnololz.summit.api.dto.lemmy.GetUnreadRegistrationApplicationCountResponse
import com.idunnololz.summit.api.dto.lemmy.HideCommunity
import com.idunnololz.summit.api.dto.lemmy.ListCommentLikesResponse
import com.idunnololz.summit.api.dto.lemmy.ListCommentReportsResponse
import com.idunnololz.summit.api.dto.lemmy.ListMediaResponse
import com.idunnololz.summit.api.dto.lemmy.ListPostLikesResponse
import com.idunnololz.summit.api.dto.lemmy.ListPostReportsResponse
import com.idunnololz.summit.api.dto.lemmy.ListPrivateMessageReportsResponse
import com.idunnololz.summit.api.dto.lemmy.LockPost
import com.idunnololz.summit.api.dto.lemmy.MarkAllAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkCommentReplyAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkPersonMentionAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkPostAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkPrivateMessageAsRead
import com.idunnololz.summit.api.dto.lemmy.PersonMentionResponse
import com.idunnololz.summit.api.dto.lemmy.PrivateMessageReportResponse
import com.idunnololz.summit.api.dto.lemmy.PurgeComment
import com.idunnololz.summit.api.dto.lemmy.PurgeCommunity
import com.idunnololz.summit.api.dto.lemmy.PurgePerson
import com.idunnololz.summit.api.dto.lemmy.PurgePost
import com.idunnololz.summit.api.dto.lemmy.RemoveComment
import com.idunnololz.summit.api.dto.lemmy.RemoveCommunity
import com.idunnololz.summit.api.dto.lemmy.RemovePost
import com.idunnololz.summit.api.dto.lemmy.ResolvePostReport
import com.idunnololz.summit.api.dto.lemmy.ResolvePrivateMessageReport
import com.idunnololz.summit.api.dto.lemmy.SaveComment
import com.idunnololz.summit.api.dto.lemmy.SavePost
import com.idunnololz.summit.api.dto.lemmy.SaveUserSettings
import com.idunnololz.summit.api.dto.lemmy.SuccessResponse
import com.idunnololz.summit.api.dto.piefed.models.BlockCommunityResponse
import com.idunnololz.summit.api.dto.piefed.models.BlockInstanceResponse
import com.idunnololz.summit.api.dto.piefed.models.CommunityResponse
import com.idunnololz.summit.api.dto.piefed.models.CreateCommentRequest
import com.idunnololz.summit.api.dto.piefed.models.CreatePostRequest
import com.idunnololz.summit.api.dto.piefed.models.EditCommentRequest
import com.idunnololz.summit.api.dto.piefed.models.EditPostRequest
import com.idunnololz.summit.api.dto.piefed.models.GetCommentReportResponse
import com.idunnololz.summit.api.dto.piefed.models.GetCommentResponse
import com.idunnololz.summit.api.dto.piefed.models.GetCommunityResponse
import com.idunnololz.summit.api.dto.piefed.models.GetModLogResponse
import com.idunnololz.summit.api.dto.piefed.models.GetPostResponse
import com.idunnololz.summit.api.dto.piefed.models.GetRegistrationListResponse
import com.idunnololz.summit.api.dto.piefed.models.GetSiteResponse
import com.idunnololz.summit.api.dto.piefed.models.GetUserResponse
import com.idunnololz.summit.api.dto.piefed.models.ImageUploadResponse
import com.idunnololz.summit.api.dto.piefed.models.ListCommentsResponse
import com.idunnololz.summit.api.dto.piefed.models.ListCommunitiesResponse
import com.idunnololz.summit.api.dto.piefed.models.ListPostsResponse
import com.idunnololz.summit.api.dto.piefed.models.ListPrivateMessagesResponse
import com.idunnololz.summit.api.dto.piefed.models.ModCommunityResponse
import com.idunnololz.summit.api.dto.piefed.models.PostReportResponse
import com.idunnololz.summit.api.dto.piefed.models.PrivateMessageResponse
import com.idunnololz.summit.api.dto.piefed.models.RegistrationApproveRequest
import com.idunnololz.summit.api.dto.piefed.models.ResolveObjectResponse
import com.idunnololz.summit.api.dto.piefed.models.SearchResponse
import com.idunnololz.summit.api.dto.piefed.models.UserBlockResponse
import com.idunnololz.summit.api.dto.piefed.models.UserLoginRequest
import com.idunnololz.summit.api.dto.piefed.models.UserLoginResponse
import com.idunnololz.summit.api.dto.piefed.models.UserMarkAllReadResponse
import com.idunnololz.summit.api.dto.piefed.models.UserMentionsResponse
import com.idunnololz.summit.api.dto.piefed.models.UserRepliesResponse
import com.idunnololz.summit.api.dto.piefed.models.UserSaveSettingsResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.QueryMap

interface PieFedApiAlpha {

  @GET("site")
  fun getSite(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetSiteResponse>

  /**
   * Get / fetch posts, with various filters.
   */
  @GET("post/list")
  fun getPosts(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ListPostsResponse>

  /**
   * Get / fetch a post.
   */
  @GET("post")
  fun getPost(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetPostResponse>

  /**
   * Log into lemmy.
   */
  @POST("user/login")
  fun login(
    @HeaderMap headers: Map<String, String>,
    @Body form: UserLoginRequest,
  ): Call<UserLoginResponse>

  /**
   * Like / vote on a post.
   */
  @POST("post/like")
  fun likePost(
    @HeaderMap headers: Map<String, String>,
    @Body form: CreatePostLike,
  ): Call<GetPostResponse>

  /**
   * Like / vote on a comment.
   */
  @POST("comment/like")
  fun likeComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: CreateCommentLike,
  ): Call<GetCommentResponse>

  @GET("comment/like/list")
  fun listCommentVotes(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ListCommentLikesResponse>

  @GET("post/like/list")
  fun listPostVotes(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ListPostLikesResponse>

  /**
   * Create a comment.
   */
  @POST("comment")
  fun createComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: CreateCommentRequest,
  ): Call<GetCommentResponse>

  /**
   * Edit a comment.
   */
  @PUT("comment")
  fun editComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: EditCommentRequest,
  ): Call<GetCommentResponse>

  /**
   * Edit a comment.
   */
  @GET("comment")
  fun getComment(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetCommentResponse>

  /**
   * Delete a comment.
   */
  @POST("comment/delete")
  fun deleteComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: DeleteComment,
  ): Call<GetCommentResponse>

  /**
   * Save a post.
   */
  @PUT("post/save")
  fun savePost(@HeaderMap headers: Map<String, String>, @Body form: SavePost): Call<GetPostResponse>

  @POST("post/mark_as_read")
  fun markPostAsRead(
    @HeaderMap headers: Map<String, String>,
    @Body form: MarkPostAsRead,
  ): Call<SuccessResponse>

  /**
   * Save a comment.
   */
  @PUT("comment/save")
  fun saveComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: SaveComment,
  ): Call<GetCommentResponse>

  /**
   * Get / fetch comments.
   */
  @GET("comment/list")
  fun getComments(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ListCommentsResponse>

  @POST("comment/distinguish")
  fun distinguishComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: DistinguishComment,
  ): Call<GetCommentResponse>

  @POST("comment/remove")
  fun removeComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: RemoveComment,
  ): Call<GetCommentResponse>

  /**
   * Get / fetch a community.
   */
  @GET("community")
  fun getCommunity(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetCommunityResponse>

  /**
   * Create a community.
   */
  @POST("community")
  fun createCommunity(
    @HeaderMap headers: Map<String, String>,
    @Body createCommunity: CreateCommunity,
  ): Call<CommunityResponse>

  /**
   * Update a community.
   */
  @PUT("community")
  fun updateCommunity(
    @HeaderMap headers: Map<String, String>,
    @Body editCommunity: EditCommunity,
  ): Call<CommunityResponse>

  /**
   * Delete a community.
   */
  @POST("community/delete")
  fun deleteCommunity(
    @HeaderMap headers: Map<String, String>,
    @Body deleteCommunity: DeleteCommunity,
  ): Call<CommunityResponse>

  /**
   * Get / fetch a community.
   */
  @GET("community/list")
  fun getCommunityList(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ListCommunitiesResponse>

  /**
   * Get the details for a person.
   */
  @GET("user")
  fun getPersonDetails(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetUserResponse>

//  @PUT("user/change_password")
//  fun changePassword(
//    @HeaderMap headers: Map<String, String>,
//    @Body form: ChangePassword,
//  ): Call<LoginResponse>

  /**
   * Get comment replies.
   */
  @GET("user/replies")
  fun getReplies(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<UserRepliesResponse>

  /**
   * Mark a comment as read.
   */
  @POST("comment/mark_as_read")
  fun markCommentReplyAsRead(
    @HeaderMap headers: Map<String, String>,
    @Body form: MarkCommentReplyAsRead,
  ): Call<SuccessResponse>

  /**
   * Mark a person mention as read.
   */
  @POST("user/mention/mark_as_read")
  fun markPersonMentionAsRead(
    @HeaderMap headers: Map<String, String>,
    @Body form: MarkPersonMentionAsRead,
  ): Call<PersonMentionResponse>

  /**
   * Mark a private message as read.
   */
  @POST("private_message/mark_as_read")
  fun markPrivateMessageAsRead(
    @HeaderMap headers: Map<String, String>,
    @Body form: MarkPrivateMessageAsRead,
  ): Call<PrivateMessageResponse>

  /**
   * Mark all replies as read.
   */
  @POST("user/mark_all_as_read")
  fun markAllAsRead(
    @HeaderMap headers: Map<String, String>,
    @Body form: MarkAllAsRead,
  ): Call<UserMarkAllReadResponse>

  /**
   * Get mentions for your user.
   */
  @GET("user/mentions")
  fun getPersonMentions(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<UserMentionsResponse>

  /**
   * Get / fetch private messages.
   */
  @GET("private_message/list")
  fun getPrivateMessages(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ListPrivateMessagesResponse>

  /**
   * These are instance wide reports that are only visible for instance admins.
   */
  @GET("private_message/report/list")
  fun getPrivateMessageReports(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ListPrivateMessageReportsResponse>

  @POST("private_message/report")
  fun createPrivateMessageReport(
    @HeaderMap headers: Map<String, String>,
    @Body form: CreatePrivateMessageReport,
  ): Call<PrivateMessageReportResponse>

  @PUT("private_message/report/resolve")
  fun resolvePrivateMessageReport(
    @HeaderMap headers: Map<String, String>,
    @Body resolvePrivateMessageReport: ResolvePrivateMessageReport,
  ): Call<PrivateMessageReportResponse>

  @GET("post/report/list")
  fun getPostReports(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ListPostReportsResponse>

  @PUT("post/report/resolve")
  fun resolvePostReport(
    @HeaderMap headers: Map<String, String>,
    @Body resolvePostReport: ResolvePostReport,
  ): Call<PostReportResponse>

  @GET("comment/report/list")
  fun getCommentReports(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ListCommentReportsResponse>

//  @PUT("comment/report/resolve")
//  fun resolveCommentReport(
//    @HeaderMap headers: Map<String, String>,
//    @Body resolveCommentReport: ResolveCommentReport,
//  ): Call<CommentReportResponse>

  /**
   * Create a private message.
   */
  @POST("private_message")
  fun createPrivateMessage(
    @HeaderMap headers: Map<String, String>,
    @Body form: CreatePrivateMessage,
  ): Call<PrivateMessageResponse>

  /**
   * Get your unread counts
   */
  @GET("user/unread_count")
  fun getUnreadCount(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetUnreadCountResponse>

  @GET("user/report_count")
  fun getReportCount(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetReportCountResponse>

  /**
   * Follow / subscribe to a community.
   */
  @POST("community/follow")
  fun followCommunity(
    @HeaderMap headers: Map<String, String>,
    @Body form: FollowCommunity,
  ): Call<CommunityResponse>

  @POST("community/ban_user")
  fun banUserFromCommunity(
    @HeaderMap headers: Map<String, String>,
    @Body banUser: BanFromCommunity,
  ): Call<BanFromCommunityResponse>

  @POST("community/mod")
  fun modUser(
    @HeaderMap headers: Map<String, String>,
    @Body modUser: AddModToCommunity,
  ): Call<ModCommunityResponse>

  /**
   * Create a post.
   */
  @POST("post")
  fun createPost(
    @HeaderMap headers: Map<String, String>,
    @Body form: CreatePostRequest,
  ): Call<GetPostResponse>

  /**
   * Edit a post.
   */
  @PUT("post")
  fun editPost(
    @HeaderMap headers: Map<String, String>,
    @Body form: EditPostRequest,
  ): Call<GetPostResponse>

  /**
   * Delete a post.
   */
  @POST("post/delete")
  fun deletePost(
    @HeaderMap headers: Map<String, String>,
    @Body form: DeletePost,
  ): Call<GetPostResponse>

  @POST("post/feature")
  fun featurePost(
    @HeaderMap headers: Map<String, String>,
    @Body form: FeaturePost,
  ): Call<GetPostResponse>

  @POST("post/lock")
  fun lockPost(
    @HeaderMap headers: Map<String, String>,
    @Body form: LockPost,
  ): Call<GetPostResponse>

  @POST("post/remove")
  fun removePost(
    @HeaderMap headers: Map<String, String>,
    @Body form: RemovePost,
  ): Call<GetPostResponse>

  /**
   * Search lemmy.
   */
  @GET("search")
  fun search(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<SearchResponse>

  /**
   * Fetch metadata for any given site.
   */
  @GET("post/site_metadata")
  fun getSiteMetadata(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetSiteMetadataResponse>

  /**
   * Report a comment.
   */
  @POST("comment/report")
  fun createCommentReport(
    @HeaderMap headers: Map<String, String>,
    @Body form: CreateCommentReport,
  ): Call<GetCommentReportResponse>

  /**
   * Report a post.
   */
  @POST("post/report")
  fun createPostReport(
    @HeaderMap headers: Map<String, String>,
    @Body form: CreatePostReport,
  ): Call<PostReportResponse>

  /**
   * Block a person.
   */
  @POST("user/block")
  fun blockPerson(
    @HeaderMap headers: Map<String, String>,
    @Body form: BlockPerson,
  ): Call<UserBlockResponse>

  /**
   * Block a community.
   */
  @POST("community/block")
  fun blockCommunity(
    @HeaderMap headers: Map<String, String>,
    @Body form: BlockCommunity,
  ): Call<BlockCommunityResponse>

  /**
   * Block an instance.
   */
  @POST("site/block")
  fun blockInstance(
    @HeaderMap headers: Map<String, String>,
    @Body form: BlockInstance,
  ): Call<BlockInstanceResponse>

  /**
   * Save your user settings.
   */
  @PUT("user/save_user_settings")
  fun saveUserSettings(
    @HeaderMap headers: Map<String, String>,
    @Body form: SaveUserSettings,
  ): Call<UserSaveSettingsResponse>

  /**
   * Upload an image.
   */
  @Multipart
  @POST("upload/image")
  fun uploadImage(
    @HeaderMap headers: Map<String, String>,
    @Header("Cookie") token: String,
    @Part filePart: MultipartBody.Part,
  ): Call<ImageUploadResponse>

  @GET("resolve_object")
  fun resolveObject(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ResolveObjectResponse>

  /**
   * Admin endpoints
   */

  @POST("user/ban")
  fun banUserFromSite(
    @HeaderMap headers: Map<String, String>,
    @Body form: BanPerson,
  ): Call<BanPersonResponse>

  @POST("community/remove")
  fun removeCommunity(
    @HeaderMap headers: Map<String, String>,
    @Body form: RemoveCommunity,
  ): Call<CommunityResponse>

  @PUT("community/hide")
  fun hideCommunity(
    @HeaderMap headers: Map<String, String>,
    @Body form: HideCommunity,
  ): Call<SuccessResponse>

  /**
   * Admin endpoints
   */

  @POST("admin/purge/person")
  fun purgePerson(
    @HeaderMap headers: Map<String, String>,
    @Body form: PurgePerson,
  ): Call<SuccessResponse>

  @POST("admin/purge/community")
  fun purgeCommunity(
    @HeaderMap headers: Map<String, String>,
    @Body form: PurgeCommunity,
  ): Call<SuccessResponse>

  @POST("admin/purge/post")
  fun purgePost(
    @HeaderMap headers: Map<String, String>,
    @Body form: PurgePost,
  ): Call<SuccessResponse>

  @POST("admin/purge/comment")
  fun purgeComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: PurgeComment,
  ): Call<SuccessResponse>

  @GET("admin/registration_application/count")
  fun getRegistrationApplicationsCount(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetUnreadRegistrationApplicationCountResponse>

  @GET("admin/registration_application/list")
  fun listRegistrationApplications(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetRegistrationListResponse>

  @PUT("admin/registration_application/approve")
  fun approveRegistrationApplication(
    @HeaderMap headers: Map<String, String>,
    @Body form: RegistrationApproveRequest,
  ): Call<Unit>

  @GET("modlog")
  fun getModLogs(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetModLogResponse>

//  @POST("user/register")
//  fun register(
//    @HeaderMap headers: Map<String, String>,
//    @Body register: Register,
//  ): Call<LoginResponse>

  @GET("user/get_captcha")
  fun getCaptcha(@HeaderMap headers: Map<String, String>): Call<GetCaptchaResponse>

  @GET("account/list_media")
  fun listMedia(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ListMediaResponse>

  // https://lemmy.world/pictrs/image/delete/b60f8360-38bd-450a-ad6c-27b0b3936a27/60ac57fb-0bdd-42af-899a-01982ad37285.jpeg
}
