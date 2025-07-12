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
import java.io.InputStream

interface LemmyLikeApi {

  val instance: String

  fun getSite(
    authorization: String?,
    args: GetSite,
    force: Boolean,
  ): Call<GetSiteResponse>

  /**
   * Get / fetch posts, with various filters.
   */
  fun getPosts(
    authorization: String?,
    args: GetPosts,
    force: Boolean,
  ): Call<GetPostsResponse>

  /**
   * Get / fetch a post.
   */
  fun getPost(
    authorization: String?,
    args: GetPost,
    force: Boolean,
  ): Call<GetPostResponse>

  /**
   * Log into lemmy.
   */
  fun login(args: Login): Call<LoginResponse>

  /**
   * Like / vote on a post.
   */
  fun likePost(
    authorization: String?,
    args: CreatePostLike,
  ): Call<PostResponse>

  /**
   * Like / vote on a comment.
   */
  fun likeComment(
    authorization: String?,
    args: CreateCommentLike,
  ): Call<CommentResponse>

  fun listCommentVotes(
    authorization: String?,
    args: ListCommentLikes,
    force: Boolean,
  ): Call<ListCommentLikesResponse>

  fun listPostVotes(
    authorization: String?,
    args: ListPostLikes,
    force: Boolean,
  ): Call<ListPostLikesResponse>

  /**
   * Create a comment.
   */
  fun createComment(
    authorization: String?,
    args: CreateComment,
  ): Call<CommentResponse>

  /**
   * Edit a comment.
   */
  fun editComment(
    authorization: String?,
    args: EditComment,
  ): Call<CommentResponse>

  /**
   * Delete a comment.
   */
  fun deleteComment(
    authorization: String?,
    args: DeleteComment,
  ): Call<CommentResponse>

  /**
   * Save a post.
   */
  fun savePost(
    authorization: String?,
    args: SavePost,
  ): Call<PostResponse>

  fun markPostAsRead(
    authorization: String?,
    args: MarkPostAsRead,
  ): Call<PostResponse>

  /**
   * Save a comment.
   */
  fun saveComment(
    authorization: String?,
    args: SaveComment,
  ): Call<CommentResponse>

  /**
   * Get / fetch comments.
   */
  fun getComments(
    authorization: String?,
    args: GetComments,
    force: Boolean,
  ): Call<GetCommentsResponse>

  fun distinguishComment(
    authorization: String?,
    args: DistinguishComment,
  ): Call<CommentResponse>

  fun removeComment(
    authorization: String?,
    args: RemoveComment,
  ): Call<CommentResponse>

  /**
   * Get / fetch a community.
   */
  fun getCommunity(
    authorization: String?,
    args: GetCommunity,
    force: Boolean,
  ): Call<GetCommunityResponse>

  /**
   * Create a community.
   */
  fun createCommunity(
    authorization: String?,
    args: CreateCommunity,
  ): Call<CommunityResponse>

  /**
   * Update a community.
   */
  fun updateCommunity(
    authorization: String?,
    args: EditCommunity,
  ): Call<CommunityResponse>

  /**
   * Delete a community.
   */
  fun deleteCommunity(
    authorization: String?,
    args: DeleteCommunity,
  ): Call<CommunityResponse>

  /**
   * Get / fetch a community.
   */
  fun getCommunityList(
    authorization: String?,
    args: ListCommunities,
    force: Boolean,
  ): Call<ListCommunitiesResponse>

  /**
   * Get the details for a person.
   */
  fun getPersonDetails(
    authorization: String?,
    args: GetPersonDetails,
    force: Boolean,
  ): Call<GetPersonDetailsResponse>

  fun changePassword(
    authorization: String?,
    args: ChangePassword,
  ): Call<LoginResponse>

  /**
   * Get comment replies.
   */
  fun getReplies(
    authorization: String?,
    args: GetReplies,
    force: Boolean,
  ): Call<GetRepliesResponse>

  /**
   * Mark a comment as read.
   */
  fun markCommentReplyAsRead(
    authorization: String?,
    args: MarkCommentReplyAsRead,
  ): Call<CommentResponse>

  /**
   * Mark a person mention as read.
   */
  fun markPersonMentionAsRead(
    authorization: String?,
    args: MarkPersonMentionAsRead,
  ): Call<PersonMentionResponse>

  /**
   * Mark a private message as read.
   */
  fun markPrivateMessageAsRead(
    authorization: String?,
    args: MarkPrivateMessageAsRead,
  ): Call<PrivateMessageResponse>

  /**
   * Mark all replies as read.
   */
  fun markAllAsRead(
    authorization: String?,
    args: MarkAllAsRead,
  ): Call<GetRepliesResponse>

  /**
   * Get mentions for your user.
   */
  fun getPersonMentions(
    authorization: String?,
    args: GetPersonMentions,
    force: Boolean,
  ): Call<GetPersonMentionsResponse>

  /**
   * Get / fetch private messages.
   */
  fun getPrivateMessages(
    authorization: String?,
    args: GetPrivateMessages,
    force: Boolean,
  ): Call<PrivateMessagesResponse>

  /**
   * These are instance wide reports that are only visible for instance admins.
   */
  fun getPrivateMessageReports(
    authorization: String?,
    args: ListPrivateMessageReports,
    force: Boolean,
  ): Call<ListPrivateMessageReportsResponse>

  fun createPrivateMessageReport(
    authorization: String?,
    args: CreatePrivateMessageReport,
  ): Call<PrivateMessageReportResponse>

  fun resolvePrivateMessageReport(
    authorization: String?,
    args: ResolvePrivateMessageReport,
  ): Call<PrivateMessageReportResponse>

  fun getPostReports(
    authorization: String?,
    args: ListPostReports,
    force: Boolean,
  ): Call<ListPostReportsResponse>

  fun resolvePostReport(
    authorization: String?,
    args: ResolvePostReport,
  ): Call<PostReportResponse>

  fun getCommentReports(
    authorization: String?,
    args: ListCommentReports,
    force: Boolean,
  ): Call<ListCommentReportsResponse>

  fun resolveCommentReport(
    authorization: String?,
    args: ResolveCommentReport,
  ): Call<CommentReportResponse>

  /**
   * Create a private message.
   */
  fun createPrivateMessage(
    authorization: String?,
    args: CreatePrivateMessage,
  ): Call<PrivateMessageResponse>

  /**
   * Get your unread counts
   */
  fun getUnreadCount(
    authorization: String?,
    args: GetUnreadCount,
    force: Boolean,
  ): Call<GetUnreadCountResponse>

  fun getReportCount(
    authorization: String?,
    args: GetReportCount,
    force: Boolean,
  ): Call<GetReportCountResponse>

  /**
   * Follow / subscribe to a community.
   */
  fun followCommunity(
    authorization: String?,
    args: FollowCommunity,
  ): Call<CommunityResponse>

  fun banUserFromCommunity(
    authorization: String?,
    args: BanFromCommunity,
  ): Call<BanFromCommunityResponse>

  fun modUser(
    authorization: String?,
    args: AddModToCommunity,
  ): Call<AddModToCommunityResponse>

  /**
   * Create a post.
   */
  fun createPost(
    authorization: String?,
    args: CreatePost,
  ): Call<PostResponse>

  /**
   * Edit a post.
   */
  fun editPost(
    authorization: String?,
    args: EditPost,
  ): Call<PostResponse>

  /**
   * Delete a post.
   */
  fun deletePost(
    authorization: String?,
    args: DeletePost,
  ): Call<PostResponse>

  fun featurePost(
    authorization: String?,
    args: FeaturePost,
  ): Call<PostResponse>

  fun lockPost(
    authorization: String?,
    args: LockPost,
  ): Call<PostResponse>

  fun removePost(
    authorization: String?,
    args: RemovePost,
  ): Call<PostResponse>

  /**
   * Search lemmy.
   */
  fun search(
    authorization: String?,
    args: Search,
    force: Boolean,
  ): Call<SearchResponse>

  /**
   * Fetch metadata for any given site.
   */
  fun getSiteMetadata(
    authorization: String?,
    args: GetSiteMetadata,
    force: Boolean,
  ): Call<GetSiteMetadataResponse>

  /**
   * Report a comment.
   */
  fun createCommentReport(
    authorization: String?,
    args: CreateCommentReport,
  ): Call<CommentReportResponse>

  /**
   * Report a post.
   */
  fun createPostReport(
    authorization: String?,
    args: CreatePostReport,
  ): Call<PostReportResponse>

  /**
   * Block a person.
   */
  fun blockPerson(
    authorization: String?,
    args: BlockPerson,
  ): Call<BlockPersonResponse>

  /**
   * Block a community.
   */
  fun blockCommunity(
    authorization: String?,
    args: BlockCommunity,
  ): Call<BlockCommunityResponse>

  /**
   * Block an instance.
   */
  fun blockInstance(
    authorization: String?,
    args: BlockInstance,
  ): Call<BlockInstanceResponse>

  /**
   * Save your user settings.
   */
  fun saveUserSettings(
    authorization: String?,
    args: SaveUserSettings,
  ): Call<LoginResponse>

  /**
   * Upload an image.
   */
  fun uploadImage(
    authorization: String?,
    url: String,
    fileName: String,
    imageIs: InputStream,
  ): Call<PictrsImages>

  fun resolveObject(
    authorization: String?,
    args: ResolveObject,
    force: Boolean,
  ): Call<ResolveObjectResponse>

  /**
   * Admin endpoints
   */

  fun banUserFromSite(
    authorization: String?,
    args: BanPerson,
  ): Call<BanPersonResponse>

  fun removeCommunity(
    authorization: String?,
    args: RemoveCommunity,
  ): Call<CommunityResponse>

  fun hideCommunity(
    authorization: String?,
    args: HideCommunity,
  ): Call<SuccessResponse>

  fun purgePerson(
    authorization: String?,
    args: PurgePerson,
  ): Call<SuccessResponse>

  fun purgeCommunity(
    authorization: String?,
    args: PurgeCommunity,
  ): Call<SuccessResponse>

  fun purgePost(
    authorization: String?,
    args: PurgePost,
  ): Call<SuccessResponse>

  fun purgeComment(
    authorization: String?,
     args: PurgeComment,
  ): Call<SuccessResponse>

  fun getRegistrationApplicationsCount(
    authorization: String?,
    args: GetUnreadRegistrationApplicationCount,
    force: Boolean,
  ): Call<GetUnreadRegistrationApplicationCountResponse>

  fun listRegistrationApplications(
    authorization: String?,
    args: ListRegistrationApplications,
    force: Boolean,
  ): Call<ListRegistrationApplicationsResponse>

  fun approveRegistrationApplication(
    authorization: String?,
     args: ApproveRegistrationApplication,
  ): Call<RegistrationApplicationResponse>

  fun getModLogs(
    authorization: String?,
    args: GetModlog,
    force: Boolean,
  ): Call<GetModlogResponse>

  fun register(args: Register): Call<LoginResponse>

  fun getCaptcha(): Call<GetCaptchaResponse>

  fun listMedia(
    authorization: String?,
    args: ListMedia,
    force: Boolean,
  ): Call<ListMediaResponse>
}