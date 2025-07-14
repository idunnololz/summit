package com.idunnololz.summit.api

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
import com.idunnololz.summit.api.dto.lemmy.GetCommentsResponse
import com.idunnololz.summit.api.dto.lemmy.GetCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.GetModlogResponse
import com.idunnololz.summit.api.dto.lemmy.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.lemmy.GetPersonMentionsResponse
import com.idunnololz.summit.api.dto.lemmy.GetPostResponse
import com.idunnololz.summit.api.dto.lemmy.GetPostsResponse
import com.idunnololz.summit.api.dto.lemmy.GetRepliesResponse
import com.idunnololz.summit.api.dto.lemmy.GetReportCountResponse
import com.idunnololz.summit.api.dto.lemmy.GetSiteMetadataResponse
import com.idunnololz.summit.api.dto.lemmy.GetSiteResponse
import com.idunnololz.summit.api.dto.lemmy.GetUnreadCountResponse
import com.idunnololz.summit.api.dto.lemmy.GetUnreadRegistrationApplicationCountResponse
import com.idunnololz.summit.api.dto.lemmy.HideCommunity
import com.idunnololz.summit.api.dto.lemmy.ListCommentLikesResponse
import com.idunnololz.summit.api.dto.lemmy.ListCommentReportsResponse
import com.idunnololz.summit.api.dto.lemmy.ListCommunitiesResponse
import com.idunnololz.summit.api.dto.lemmy.ListMediaResponse
import com.idunnololz.summit.api.dto.lemmy.ListPostLikesResponse
import com.idunnololz.summit.api.dto.lemmy.ListPostReportsResponse
import com.idunnololz.summit.api.dto.lemmy.ListPrivateMessageReportsResponse
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
import com.idunnololz.summit.api.dto.lemmy.PictrsImages
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
import com.idunnololz.summit.api.dto.lemmy.ResolveObjectResponse
import com.idunnololz.summit.api.dto.lemmy.ResolvePostReport
import com.idunnololz.summit.api.dto.lemmy.ResolvePrivateMessageReport
import com.idunnololz.summit.api.dto.lemmy.SaveComment
import com.idunnololz.summit.api.dto.lemmy.SavePost
import com.idunnololz.summit.api.dto.lemmy.SaveUserSettings
import com.idunnololz.summit.api.dto.lemmy.SearchResponse
import com.idunnololz.summit.api.dto.lemmy.SuccessResponse
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
import retrofit2.http.Url

interface LemmyApiV3 {

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
  ): Call<GetPostsResponse>

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
    @Body form: Login
  ): Call<LoginResponse>

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
  fun savePost(
    @HeaderMap headers: Map<String, String>,
    @Body form: SavePost,
  ): Call<PostResponse>

  @POST("post/mark_as_read")
  fun markPostAsRead(
    @HeaderMap headers: Map<String, String>,
    @Body form: MarkPostAsRead,
  ): Call<PostResponse>

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
  ): Call<GetCommentsResponse>

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
  ): Call<ListCommunitiesResponse>

  /**
   * Get the details for a person.
   */
  @GET("user")
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
   * Get comment replies.
   */
  @GET("user/replies")
  fun getReplies(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetRepliesResponse>

  /**
   * Mark a comment as read.
   */
  @POST("comment/mark_as_read")
  fun markCommentReplyAsRead(
    @HeaderMap headers: Map<String, String>,
    @Body form: MarkCommentReplyAsRead,
  ): Call<CommentResponse>

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
  ): Call<GetRepliesResponse>

  /**
   * Get mentions for your user.
   */
  @GET("user/mention")
  fun getPersonMentions(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetPersonMentionsResponse>

  /**
   * Get / fetch private messages.
   */
  @GET("private_message/list")
  fun getPrivateMessages(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<PrivateMessagesResponse>

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
  fun editPost(
    @HeaderMap headers: Map<String, String>,
    @Body form: EditPost,
  ): Call<PostResponse>

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
  fun lockPost(
    @HeaderMap headers: Map<String, String>,
    @Body form: LockPost,
  ): Call<PostResponse>

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
  @POST("user/block")
  fun blockPerson(
    @HeaderMap headers: Map<String, String>,
    @Body form: BlockPerson,
  ): Call<BlockPersonResponse>

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
  ): Call<PictrsImages>

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
  ): Call<ListRegistrationApplicationsResponse>

  @PUT("admin/registration_application/approve")
  fun approveRegistrationApplication(
    @HeaderMap headers: Map<String, String>,
    @Body form: ApproveRegistrationApplication,
  ): Call<RegistrationApplicationResponse>

  @GET("modlog")
  fun getModLogs(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<GetModlogResponse>

  @POST("user/register")
  fun register(
    @HeaderMap headers: Map<String, String>,
    @Body register: Register
  ): Call<LoginResponse>

  @GET("user/get_captcha")
  fun getCaptcha(
    @HeaderMap headers: Map<String, String>,
  ): Call<GetCaptchaResponse>

  @GET("account/list_media")
  fun listMedia(
    @HeaderMap headers: Map<String, String>,
    @QueryMap form: Map<String, String>,
  ): Call<ListMediaResponse>

  // https://lemmy.world/pictrs/image/delete/b60f8360-38bd-450a-ad6c-27b0b3936a27/60ac57fb-0bdd-42af-899a-01982ad37285.jpeg

}
