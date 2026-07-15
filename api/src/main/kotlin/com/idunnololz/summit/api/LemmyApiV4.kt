package com.idunnololz.summit.api

import com.idunnololz.summit.api.dto.lemmy.v4.models.AddModToCommunity
import com.idunnololz.summit.api.dto.lemmy.v4.models.AddModToCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.ApproveRegistrationApplication
import com.idunnololz.summit.api.dto.lemmy.v4.models.BanFromCommunity
import com.idunnololz.summit.api.dto.lemmy.v4.models.BanPerson
import com.idunnololz.summit.api.dto.lemmy.v4.models.BlockCommunity
import com.idunnololz.summit.api.dto.lemmy.v4.models.BlockPerson
import com.idunnololz.summit.api.dto.lemmy.v4.models.ChangePassword
import com.idunnololz.summit.api.dto.lemmy.v4.models.CommentReportResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.CommentResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.CommunityResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.CreateComment
import com.idunnololz.summit.api.dto.lemmy.v4.models.CreateCommentLike
import com.idunnololz.summit.api.dto.lemmy.v4.models.CreateCommentReport
import com.idunnololz.summit.api.dto.lemmy.v4.models.CreateCommunity
import com.idunnololz.summit.api.dto.lemmy.v4.models.CreatePost
import com.idunnololz.summit.api.dto.lemmy.v4.models.CreatePostLike
import com.idunnololz.summit.api.dto.lemmy.v4.models.CreatePostReport
import com.idunnololz.summit.api.dto.lemmy.v4.models.CreatePrivateMessage
import com.idunnololz.summit.api.dto.lemmy.v4.models.CreatePrivateMessageReport
import com.idunnololz.summit.api.dto.lemmy.v4.models.DeleteComment
import com.idunnololz.summit.api.dto.lemmy.v4.models.DeleteCommunity
import com.idunnololz.summit.api.dto.lemmy.v4.models.DeleteImageParamsI
import com.idunnololz.summit.api.dto.lemmy.v4.models.DeletePost
import com.idunnololz.summit.api.dto.lemmy.v4.models.DistinguishComment
import com.idunnololz.summit.api.dto.lemmy.v4.models.EditComment
import com.idunnololz.summit.api.dto.lemmy.v4.models.EditCommunity
import com.idunnololz.summit.api.dto.lemmy.v4.models.EditPost
import com.idunnololz.summit.api.dto.lemmy.v4.models.FeaturePost
import com.idunnololz.summit.api.dto.lemmy.v4.models.FollowCommunity
import com.idunnololz.summit.api.dto.lemmy.v4.models.GetCaptchaResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.GetCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.GetPostResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.GetSiteMetadataResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.GetSiteResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.HideCommunity
import com.idunnololz.summit.api.dto.lemmy.v4.models.LockPost
import com.idunnololz.summit.api.dto.lemmy.v4.models.Login
import com.idunnololz.summit.api.dto.lemmy.v4.models.LoginResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.MarkNotificationAsRead
import com.idunnololz.summit.api.dto.lemmy.v4.models.MarkPostAsRead
import com.idunnololz.summit.api.dto.lemmy.v4.models.PagedResponseCommentView
import com.idunnololz.summit.api.dto.lemmy.v4.models.PagedResponseCommunityView
import com.idunnololz.summit.api.dto.lemmy.v4.models.PagedResponseFederatedInstanceView
import com.idunnololz.summit.api.dto.lemmy.v4.models.PagedResponseLocalImageView
import com.idunnololz.summit.api.dto.lemmy.v4.models.PagedResponseModlogView
import com.idunnololz.summit.api.dto.lemmy.v4.models.PagedResponseNotificationView
import com.idunnololz.summit.api.dto.lemmy.v4.models.PagedResponsePostView
import com.idunnololz.summit.api.dto.lemmy.v4.models.PagedResponseRegistrationApplicationView
import com.idunnololz.summit.api.dto.lemmy.v4.models.PagedResponseReportCombinedView
import com.idunnololz.summit.api.dto.lemmy.v4.models.PagedResponseVoteView
import com.idunnololz.summit.api.dto.lemmy.v4.models.PersonResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.PostReportResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.PostResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.PrivateMessageReportResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.PrivateMessageResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.PurgeComment
import com.idunnololz.summit.api.dto.lemmy.v4.models.PurgeCommunity
import com.idunnololz.summit.api.dto.lemmy.v4.models.PurgePerson
import com.idunnololz.summit.api.dto.lemmy.v4.models.PurgePost
import com.idunnololz.summit.api.dto.lemmy.v4.models.Register
import com.idunnololz.summit.api.dto.lemmy.v4.models.RegistrationApplicationResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.RemoveComment
import com.idunnololz.summit.api.dto.lemmy.v4.models.RemoveCommunity
import com.idunnololz.summit.api.dto.lemmy.v4.models.RemovePost
import com.idunnololz.summit.api.dto.lemmy.v4.models.RequestStateCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.ResolveCommentReport
import com.idunnololz.summit.api.dto.lemmy.v4.models.ResolveObjectView
import com.idunnololz.summit.api.dto.lemmy.v4.models.ResolvePostReport
import com.idunnololz.summit.api.dto.lemmy.v4.models.ResolvePrivateMessageReport
import com.idunnololz.summit.api.dto.lemmy.v4.models.SaveComment
import com.idunnololz.summit.api.dto.lemmy.v4.models.SavePost
import com.idunnololz.summit.api.dto.lemmy.v4.models.SaveUserSettings
import com.idunnololz.summit.api.dto.lemmy.v4.models.SearchResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.SuccessResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.UnreadCountsResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.UploadImageResponse
import com.idunnololz.summit.api.dto.lemmy.v4.models.UserBlockInstanceCommunitiesParams
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface LemmyApiV4 {

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
  ): Call<PagedResponsePostView>

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
  fun login(@HeaderMap headers: Map<String, String>, @Body form: Login): Call<LoginResponse>

  /**
   * Like / vote on a post.
   */
  @POST("post/like")
  fun likePost(
    @HeaderMap headers: Map<String, String>,
    @Body form: CreatePostLike,
  ): Call<PostResponse>

  /**
   * Like / vote on a comment.
   */
  @POST("comment/like")
  fun likeComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: CreateCommentLike,
  ): Call<CommentResponse>

  @GET("comment/like/list")
  fun listCommentVotes(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<PagedResponseVoteView>

  @GET("post/like/list")
  fun listPostVotes(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<PagedResponseVoteView>

  /**
   * Create a comment.
   */
  @POST("comment")
  fun createComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: CreateComment,
  ): Call<CommentResponse>

  /**
   * Edit a comment.
   */
  @PUT("comment")
  fun editComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: EditComment,
  ): Call<CommentResponse>

  /**
   * Delete a comment.
   */
  @POST("comment/delete")
  fun deleteComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: DeleteComment,
  ): Call<CommentResponse>

  /**
   * Save a post.
   */
  @PUT("post/save")
  fun savePost(@HeaderMap headers: Map<String, String>, @Body form: SavePost): Call<PostResponse>

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
  ): Call<CommentResponse>

  /**
   * Get / fetch comments.
   */
  @GET("comment/list")
  fun getComments(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<PagedResponseCommentView>

  @POST("comment/distinguish")
  fun distinguishComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: DistinguishComment,
  ): Call<CommentResponse>

  @POST("comment/remove")
  fun removeComment(
    @HeaderMap headers: Map<String, String>,
    @Body form: RemoveComment,
  ): Call<CommentResponse>

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
  ): Call<PagedResponseCommunityView>

  /**
   * Get the details for a person.
   */
  @GET("account")
  fun getPersonDetails(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetPersonDetailsResponse>

  @PUT("user/change_password")
  fun changePassword(
    @HeaderMap headers: Map<String, String>,
    @Body form: ChangePassword,
  ): Call<LoginResponse>

  /**
   * Get notification/inbox items (replies, comment mentions, post mentions, and messages).
   */
  @GET("account/notification/list")
  fun listInbox(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<PagedResponseNotificationView>

  /**
   * Mark a notification as read.
   */
  @POST("account/notification/mark_as_read")
  fun markNotificationAsRead(
    @HeaderMap headers: Map<String, String>,
    @Body form: MarkNotificationAsRead,
  ): Call<SuccessResponse>

  /**
   * Mark all notifications as read.
   */
  @POST("account/notification/mark_as_read/all")
  fun markAllAsRead(
    @HeaderMap headers: Map<String, String>,
  ): Call<SuccessResponse>

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

  @GET("report/list")
  fun getReports(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<PagedResponseReportCombinedView>

  @PUT("post/report/resolve")
  fun resolvePostReport(
    @HeaderMap headers: Map<String, String>,
    @Body resolvePostReport: ResolvePostReport,
  ): Call<PostReportResponse>

  @PUT("comment/report/resolve")
  fun resolveCommentReport(
    @HeaderMap headers: Map<String, String>,
    @Body resolveCommentReport: ResolveCommentReport,
  ): Call<CommentReportResponse>

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
  @GET("account/unread_counts")
  fun getUnreadCount(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<UnreadCountsResponse>

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
  ): Call<PersonResponse>

  @POST("community/mod")
  fun modUser(
    @HeaderMap headers: Map<String, String>,
    @Body modUser: AddModToCommunity,
  ): Call<AddModToCommunityResponse>

  /**
   * Create a post.
   */
  @POST("post")
  fun createPost(
    @HeaderMap headers: Map<String, String>,
    @Body form: CreatePost,
  ): Call<PostResponse>

  /**
   * Edit a post.
   */
  @PUT("post")
  fun editPost(@HeaderMap headers: Map<String, String>, @Body form: EditPost): Call<PostResponse>

  /**
   * Delete a post.
   */
  @POST("post/delete")
  fun deletePost(
    @HeaderMap headers: Map<String, String>,
    @Body form: DeletePost,
  ): Call<PostResponse>

  @POST("post/feature")
  fun featurePost(
    @HeaderMap headers: Map<String, String>,
    @Body form: FeaturePost,
  ): Call<PostResponse>

  @POST("post/lock")
  fun lockPost(@HeaderMap headers: Map<String, String>, @Body form: LockPost): Call<PostResponse>

  @POST("post/remove")
  fun removePost(
    @HeaderMap headers: Map<String, String>,
    @Body form: RemovePost,
  ): Call<PostResponse>

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
  ): Call<CommentReportResponse>

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
  @POST("account/block/person")
  fun blockPerson(
    @HeaderMap headers: Map<String, String>,
    @Body form: BlockPerson,
  ): Call<PersonResponse>

  /**
   * Block a community.
   */
  @POST("community/block")
  fun blockCommunity(
    @HeaderMap headers: Map<String, String>,
    @Body form: BlockCommunity,
  ): Call<RequestStateCommunityResponse>

  /**
   * Block an instance.
   */
  @POST("account/block/instance/communities")
  fun blockInstance(
    @HeaderMap headers: Map<String, String>,
    @Body form: UserBlockInstanceCommunitiesParams,
  ): Call<SuccessResponse>

  /**
   * Save your user settings.
   */
  @PUT("user/save_user_settings")
  fun saveUserSettings(
    @HeaderMap headers: Map<String, String>,
    @Body form: SaveUserSettings,
  ): Call<LoginResponse>

  /**
   * Upload an image.
   */
  @Multipart
  @POST
  fun uploadImage(
    @HeaderMap headers: Map<String, String>,
    @Header("Cookie") token: String,
    @Url url: String,
    @Part filePart: MultipartBody.Part,
  ): Call<UploadImageResponse>

  @GET("resolve_object")
  fun resolveObject(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ResolveObjectView>

  /**
   * Admin endpoints
   */

  @POST("user/ban")
  fun banUserFromSite(
    @HeaderMap headers: Map<String, String>,
    @Body form: BanPerson,
  ): Call<PersonResponse>

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

  @GET("admin/registration_application/list")
  fun listRegistrationApplications(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<PagedResponseRegistrationApplicationView>

  @PUT("admin/registration_application/approve")
  fun approveRegistrationApplication(
    @HeaderMap headers: Map<String, String>,
    @Body form: ApproveRegistrationApplication,
  ): Call<RegistrationApplicationResponse>

  @GET("modlog")
  fun getModLogs(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<PagedResponseModlogView>

  @POST("user/register")
  fun register(
    @HeaderMap headers: Map<String, String>,
    @Body register: Register,
  ): Call<LoginResponse>

  @GET("user/get_captcha")
  fun getCaptcha(@HeaderMap headers: Map<String, String>): Call<GetCaptchaResponse>

  @GET("account/media/list")
  fun listMedia(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<PagedResponseLocalImageView>

//  @DELETE("account")
//  fun deleteAccount()

  @GET("federated_instances")
  fun federatedInstances(
    @HeaderMap headers: Map<String, String>,
  ): Call<PagedResponseFederatedInstanceView>

  @DELETE("account/media")
  fun deleteMedia(@HeaderMap headers: Map<String, String>, @Body body: DeleteImageParamsI): Call<Unit>
}
