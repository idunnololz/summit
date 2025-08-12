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

interface LemmyLikeApi : ApiCompat {

  val instance: String

  suspend fun getSite(
    authorization: String?,
    args: GetSite,
    force: Boolean,
  ): Result<GetSiteResponse>

  /**
   * Get / fetch posts, with various filters.
   */
  suspend fun getPosts(
    authorization: String?,
    args: GetPosts,
    force: Boolean,
  ): Result<GetPostsResponse>

  /**
   * Get / fetch a post.
   */
  suspend fun getPost(
    authorization: String?,
    args: GetPost,
    force: Boolean,
  ): Result<GetPostResponse>

  /**
   * Log into lemmy.
   */
  suspend fun login(args: Login): Result<LoginResponse>

  /**
   * Like / vote on a post.
   */
  suspend fun likePost(authorization: String?, args: CreatePostLike): Result<PostResponse>

  /**
   * Like / vote on a comment.
   */
  suspend fun likeComment(authorization: String?, args: CreateCommentLike): Result<CommentResponse>

  suspend fun listCommentVotes(
    authorization: String?,
    args: ListCommentLikes,
    force: Boolean,
  ): Result<ListCommentLikesResponse>

  suspend fun listPostVotes(
    authorization: String?,
    args: ListPostLikes,
    force: Boolean,
  ): Result<ListPostLikesResponse>

  /**
   * Create a comment.
   */
  suspend fun createComment(authorization: String?, args: CreateComment): Result<CommentResponse>

  /**
   * Edit a comment.
   */
  suspend fun editComment(authorization: String?, args: EditComment): Result<CommentResponse>

  /**
   * Delete a comment.
   */
  suspend fun deleteComment(authorization: String?, args: DeleteComment): Result<CommentResponse>

  /**
   * Save a post.
   */
  suspend fun savePost(authorization: String?, args: SavePost): Result<PostResponse>

  suspend fun markPostAsRead(authorization: String?, args: MarkPostAsRead): Result<SuccessResponse>

  /**
   * Save a comment.
   */
  suspend fun saveComment(authorization: String?, args: SaveComment): Result<CommentResponse>

  /**
   * Get / fetch comments.
   */
  suspend fun getComments(
    authorization: String?,
    args: GetComments,
    force: Boolean,
  ): Result<GetCommentsResponse>

  suspend fun distinguishComment(
    authorization: String?,
    args: DistinguishComment,
  ): Result<CommentResponse>

  suspend fun removeComment(authorization: String?, args: RemoveComment): Result<CommentResponse>

  /**
   * Get / fetch a community.
   */
  suspend fun getCommunity(
    authorization: String?,
    args: GetCommunity,
    force: Boolean,
  ): Result<GetCommunityResponse>

  /**
   * Create a community.
   */
  suspend fun createCommunity(
    authorization: String?,
    args: CreateCommunity,
  ): Result<CommunityResponse>

  /**
   * Update a community.
   */
  suspend fun updateCommunity(
    authorization: String?,
    args: EditCommunity,
  ): Result<CommunityResponse>

  /**
   * Delete a community.
   */
  suspend fun deleteCommunity(
    authorization: String?,
    args: DeleteCommunity,
  ): Result<CommunityResponse>

  /**
   * Get / fetch a community.
   */
  suspend fun getCommunityList(
    authorization: String?,
    args: ListCommunities,
    force: Boolean,
  ): Result<ListCommunitiesResponse>

  /**
   * Get the details for a person.
   */
  suspend fun getPersonDetails(
    authorization: String?,
    args: GetPersonDetails,
    force: Boolean,
  ): Result<GetPersonDetailsResponse>

  suspend fun changePassword(authorization: String?, args: ChangePassword): Result<LoginResponse>

  /**
   * Get comment replies.
   */
  suspend fun getReplies(
    authorization: String?,
    args: GetReplies,
    force: Boolean,
  ): Result<GetRepliesResponse>

  /**
   * Mark a comment as read.
   */
  suspend fun markCommentReplyAsRead(
    authorization: String?,
    args: MarkCommentReplyAsRead,
  ): Result<SuccessResponse>

  /**
   * Mark a person mention as read.
   */
  suspend fun markPersonMentionAsRead(
    authorization: String?,
    args: MarkPersonMentionAsRead,
  ): Result<PersonMentionResponse>

  /**
   * Mark a private message as read.
   */
  suspend fun markPrivateMessageAsRead(
    authorization: String?,
    args: MarkPrivateMessageAsRead,
  ): Result<PrivateMessageResponse>

  /**
   * Mark all replies as read.
   */
  suspend fun markAllAsRead(
    authorization: String?,
    args: MarkAllAsRead,
  ): Result<GetRepliesResponse>

  /**
   * Get mentions for your user.
   */
  suspend fun getPersonMentions(
    authorization: String?,
    args: GetPersonMentions,
    force: Boolean,
  ): Result<GetPersonMentionsResponse>

  /**
   * Get / fetch private messages.
   */
  suspend fun getPrivateMessages(
    authorization: String?,
    args: GetPrivateMessages,
    force: Boolean,
  ): Result<PrivateMessagesResponse>

  /**
   * These are instance wide reports that are only visible for instance admins.
   */
  suspend fun getPrivateMessageReports(
    authorization: String?,
    args: ListPrivateMessageReports,
    force: Boolean,
  ): Result<ListPrivateMessageReportsResponse>

  suspend fun createPrivateMessageReport(
    authorization: String?,
    args: CreatePrivateMessageReport,
  ): Result<PrivateMessageReportResponse>

  suspend fun resolvePrivateMessageReport(
    authorization: String?,
    args: ResolvePrivateMessageReport,
  ): Result<PrivateMessageReportResponse>

  suspend fun getPostReports(
    authorization: String?,
    args: ListPostReports,
    force: Boolean,
  ): Result<ListPostReportsResponse>

  suspend fun resolvePostReport(
    authorization: String?,
    args: ResolvePostReport,
  ): Result<PostReportResponse>

  suspend fun getCommentReports(
    authorization: String?,
    args: ListCommentReports,
    force: Boolean,
  ): Result<ListCommentReportsResponse>

  suspend fun resolveCommentReport(
    authorization: String?,
    args: ResolveCommentReport,
  ): Result<CommentReportResponse>

  /**
   * Create a private message.
   */
  suspend fun createPrivateMessage(
    authorization: String?,
    args: CreatePrivateMessage,
  ): Result<PrivateMessageResponse>

  /**
   * Get your unread counts
   */
  suspend fun getUnreadCount(
    authorization: String?,
    args: GetUnreadCount,
    force: Boolean,
  ): Result<GetUnreadCountResponse>

  suspend fun getReportCount(
    authorization: String?,
    args: GetReportCount,
    force: Boolean,
  ): Result<GetReportCountResponse>

  /**
   * Follow / subscribe to a community.
   */
  suspend fun followCommunity(
    authorization: String?,
    args: FollowCommunity,
  ): Result<CommunityResponse>

  suspend fun banUserFromCommunity(
    authorization: String?,
    args: BanFromCommunity,
  ): Result<BanFromCommunityResponse>

  suspend fun modUser(
    authorization: String?,
    args: AddModToCommunity,
  ): Result<AddModToCommunityResponse>

  /**
   * Create a post.
   */
  suspend fun createPost(authorization: String?, args: CreatePost): Result<PostResponse>

  /**
   * Edit a post.
   */
  suspend fun editPost(authorization: String?, args: EditPost): Result<PostResponse>

  /**
   * Delete a post.
   */
  suspend fun deletePost(authorization: String?, args: DeletePost): Result<PostResponse>

  suspend fun featurePost(authorization: String?, args: FeaturePost): Result<PostResponse>

  suspend fun lockPost(authorization: String?, args: LockPost): Result<PostResponse>

  suspend fun removePost(authorization: String?, args: RemovePost): Result<PostResponse>

  /**
   * Search lemmy.
   */
  suspend fun search(authorization: String?, args: Search, force: Boolean): Result<SearchResponse>

  /**
   * Fetch metadata for any given site.
   */
  suspend fun getSiteMetadata(
    authorization: String?,
    args: GetSiteMetadata,
    force: Boolean,
  ): Result<GetSiteMetadataResponse>

  /**
   * Report a comment.
   */
  suspend fun createCommentReport(
    authorization: String?,
    args: CreateCommentReport,
  ): Result<CommentReportResponse>

  /**
   * Report a post.
   */
  suspend fun createPostReport(
    authorization: String?,
    args: CreatePostReport,
  ): Result<PostReportResponse>

  /**
   * Block a person.
   */
  suspend fun blockPerson(authorization: String?, args: BlockPerson): Result<BlockPersonResponse>

  /**
   * Block a community.
   */
  suspend fun blockCommunity(
    authorization: String?,
    args: BlockCommunity,
  ): Result<BlockCommunityResponse>

  /**
   * Block an instance.
   */
  suspend fun blockInstance(
    authorization: String?,
    args: BlockInstance,
  ): Result<BlockInstanceResponse>

  /**
   * Save your user settings.
   */
  suspend fun saveUserSettings(
    authorization: String?,
    args: SaveUserSettings,
  ): Result<LoginResponse>

  /**
   * Upload an image.
   */
  suspend fun uploadImage(
    authorization: String?,
    url: String,
    fileName: String,
    imageIs: InputStream,
    mimeType: String?,
  ): Result<UploadImageResult>

  suspend fun resolveObject(
    authorization: String?,
    args: ResolveObject,
    force: Boolean,
  ): Result<ResolveObjectResponse>

  /**
   * Admin endpoints
   */

  suspend fun banUserFromSite(authorization: String?, args: BanPerson): Result<BanPersonResponse>

  suspend fun removeCommunity(
    authorization: String?,
    args: RemoveCommunity,
  ): Result<CommunityResponse>

  suspend fun hideCommunity(authorization: String?, args: HideCommunity): Result<SuccessResponse>

  suspend fun purgePerson(authorization: String?, args: PurgePerson): Result<SuccessResponse>

  suspend fun purgeCommunity(authorization: String?, args: PurgeCommunity): Result<SuccessResponse>

  suspend fun purgePost(authorization: String?, args: PurgePost): Result<SuccessResponse>

  suspend fun purgeComment(authorization: String?, args: PurgeComment): Result<SuccessResponse>

  suspend fun getRegistrationApplicationsCount(
    authorization: String?,
    args: GetUnreadRegistrationApplicationCount,
    force: Boolean,
  ): Result<GetUnreadRegistrationApplicationCountResponse>

  suspend fun listRegistrationApplications(
    authorization: String?,
    args: ListRegistrationApplications,
    force: Boolean,
  ): Result<ListRegistrationApplicationsResponse>

  suspend fun approveRegistrationApplication(
    authorization: String?,
    args: ApproveRegistrationApplication,
  ): Result<RegistrationApplicationResponse>

  suspend fun getModLogs(
    authorization: String?,
    args: GetModlog,
    force: Boolean,
  ): Result<GetModlogResponse>

  suspend fun register(args: Register): Result<LoginResponse>

  suspend fun getCaptcha(): Result<GetCaptchaResponse>

  suspend fun listMedia(
    authorization: String?,
    args: ListMedia,
    force: Boolean,
  ): Result<ListMediaResponse>

  suspend fun deleteMedia(authorization: String?, args: DeleteImage): Result<Unit>
}
