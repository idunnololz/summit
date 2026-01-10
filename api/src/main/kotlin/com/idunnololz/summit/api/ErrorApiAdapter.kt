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

class ErrorApiAdapter(
  override val instance: String,
  var error: Throwable,
  var apiInfo: ApiInfo = ApiInfo(null, null, true)
) : LemmyLikeApi {
  override suspend fun getSite(
    authorization: String?,
    args: GetSite,
    force: Boolean,
  ): Result<GetSiteResponse> =
    Result.failure(error)

  override suspend fun getPosts(
    authorization: String?,
    args: GetPosts,
    force: Boolean,
  ): Result<GetPostsResponse> =
    Result.failure(error)

  override suspend fun getPost(
    authorization: String?,
    args: GetPost,
    force: Boolean,
  ): Result<GetPostResponse> =
    Result.failure(error)

  override suspend fun login(args: Login): Result<LoginResponse> =
    Result.failure(error)

  override suspend fun likePost(
    authorization: String?,
    args: CreatePostLike,
  ): Result<PostResponse> =
    Result.failure(error)

  override suspend fun likeComment(
    authorization: String?,
    args: CreateCommentLike,
  ): Result<CommentResponse> =
    Result.failure(error)

  override suspend fun listCommentVotes(
    authorization: String?,
    args: ListCommentLikes,
    force: Boolean,
  ): Result<ListCommentLikesResponse> =
    Result.failure(error)

  override suspend fun listPostVotes(
    authorization: String?,
    args: ListPostLikes,
    force: Boolean,
  ): Result<ListPostLikesResponse> =
    Result.failure(error)

  override suspend fun createComment(
    authorization: String?,
    args: CreateComment,
  ): Result<CommentResponse> =
    Result.failure(error)

  override suspend fun editComment(
    authorization: String?,
    args: EditComment,
  ): Result<CommentResponse> =
    Result.failure(error)

  override suspend fun deleteComment(
    authorization: String?,
    args: DeleteComment,
  ): Result<CommentResponse> =
    Result.failure(error)

  override suspend fun savePost(
    authorization: String?,
    args: SavePost,
  ): Result<PostResponse> =
    Result.failure(error)

  override suspend fun markPostAsRead(
    authorization: String?,
    args: MarkPostAsRead,
  ): Result<SuccessResponse> =
    Result.failure(error)

  override suspend fun saveComment(
    authorization: String?,
    args: SaveComment,
  ): Result<CommentResponse> =
    Result.failure(error)

  override suspend fun getComments(
    authorization: String?,
    args: GetComments,
    force: Boolean,
  ): Result<GetCommentsResponse> =
    Result.failure(error)

  override suspend fun distinguishComment(
    authorization: String?,
    args: DistinguishComment,
  ): Result<CommentResponse> =
    Result.failure(error)

  override suspend fun removeComment(
    authorization: String?,
    args: RemoveComment,
  ): Result<CommentResponse> =
    Result.failure(error)

  override suspend fun getCommunity(
    authorization: String?,
    args: GetCommunity,
    force: Boolean,
  ): Result<GetCommunityResponse> =
    Result.failure(error)

  override suspend fun createCommunity(
    authorization: String?,
    args: CreateCommunity,
  ): Result<CommunityResponse> =
    Result.failure(error)

  override suspend fun updateCommunity(
    authorization: String?,
    args: EditCommunity,
  ): Result<CommunityResponse> =
    Result.failure(error)

  override suspend fun deleteCommunity(
    authorization: String?,
    args: DeleteCommunity,
  ): Result<CommunityResponse> =
    Result.failure(error)

  override suspend fun getCommunityList(
    authorization: String?,
    args: ListCommunities,
    force: Boolean,
  ): Result<ListCommunitiesResponse> =
    Result.failure(error)

  override suspend fun getPersonDetails(
    authorization: String?,
    args: GetPersonDetails,
    force: Boolean,
  ): Result<GetPersonDetailsResponse> =
    Result.failure(error)

  override suspend fun changePassword(
    authorization: String?,
    args: ChangePassword,
  ): Result<LoginResponse> =
    Result.failure(error)

  override suspend fun getReplies(
    authorization: String?,
    args: GetReplies,
    force: Boolean,
  ): Result<GetRepliesResponse> =
    Result.failure(error)

  override suspend fun markCommentReplyAsRead(
    authorization: String?,
    args: MarkCommentReplyAsRead,
  ): Result<SuccessResponse> =
    Result.failure(error)

  override suspend fun markPersonMentionAsRead(
    authorization: String?,
    args: MarkPersonMentionAsRead,
  ): Result<PersonMentionResponse> =
    Result.failure(error)

  override suspend fun markPrivateMessageAsRead(
    authorization: String?,
    args: MarkPrivateMessageAsRead,
  ): Result<PrivateMessageResponse> =
    Result.failure(error)

  override suspend fun markAllAsRead(
    authorization: String?,
    args: MarkAllAsRead,
  ): Result<GetRepliesResponse> =
    Result.failure(error)

  override suspend fun getPersonMentions(
    authorization: String?,
    args: GetPersonMentions,
    force: Boolean,
  ): Result<GetPersonMentionsResponse> =
    Result.failure(error)

  override suspend fun getPrivateMessages(
    authorization: String?,
    args: GetPrivateMessages,
    force: Boolean,
  ): Result<PrivateMessagesResponse> =
    Result.failure(error)

  override suspend fun getPrivateMessageReports(
    authorization: String?,
    args: ListPrivateMessageReports,
    force: Boolean,
  ): Result<ListPrivateMessageReportsResponse> =
    Result.failure(error)

  override suspend fun createPrivateMessageReport(
    authorization: String?,
    args: CreatePrivateMessageReport,
  ): Result<PrivateMessageReportResponse> =
    Result.failure(error)

  override suspend fun resolvePrivateMessageReport(
    authorization: String?,
    args: ResolvePrivateMessageReport,
  ): Result<PrivateMessageReportResponse> =
    Result.failure(error)

  override suspend fun getPostReports(
    authorization: String?,
    args: ListPostReports,
    force: Boolean,
  ): Result<ListPostReportsResponse> =
    Result.failure(error)

  override suspend fun resolvePostReport(
    authorization: String?,
    args: ResolvePostReport,
  ): Result<PostReportResponse> =
    Result.failure(error)

  override suspend fun getCommentReports(
    authorization: String?,
    args: ListCommentReports,
    force: Boolean,
  ): Result<ListCommentReportsResponse> =
    Result.failure(error)

  override suspend fun resolveCommentReport(
    authorization: String?,
    args: ResolveCommentReport,
  ): Result<CommentReportResponse> =
    Result.failure(error)

  override suspend fun createPrivateMessage(
    authorization: String?,
    args: CreatePrivateMessage,
  ): Result<PrivateMessageResponse> =
    Result.failure(error)

  override suspend fun getUnreadCount(
    authorization: String?,
    args: GetUnreadCount,
    force: Boolean,
  ): Result<GetUnreadCountResponse> =
    Result.failure(error)

  override suspend fun getReportCount(
    authorization: String?,
    args: GetReportCount,
    force: Boolean,
  ): Result<GetReportCountResponse> =
    Result.failure(error)

  override suspend fun followCommunity(
    authorization: String?,
    args: FollowCommunity,
  ): Result<CommunityResponse> =
    Result.failure(error)

  override suspend fun banUserFromCommunity(
    authorization: String?,
    args: BanFromCommunity,
  ): Result<BanFromCommunityResponse> =
    Result.failure(error)

  override suspend fun modUser(
    authorization: String?,
    args: AddModToCommunity,
  ): Result<AddModToCommunityResponse> =
    Result.failure(error)

  override suspend fun createPost(
    authorization: String?,
    args: CreatePost,
  ): Result<PostResponse> =
    Result.failure(error)

  override suspend fun editPost(
    authorization: String?,
    args: EditPost,
  ): Result<PostResponse> =
    Result.failure(error)

  override suspend fun deletePost(
    authorization: String?,
    args: DeletePost,
  ): Result<PostResponse> =
    Result.failure(error)

  override suspend fun featurePost(
    authorization: String?,
    args: FeaturePost,
  ): Result<PostResponse> =
    Result.failure(error)

  override suspend fun lockPost(
    authorization: String?,
    args: LockPost,
  ): Result<PostResponse> =
    Result.failure(error)

  override suspend fun removePost(
    authorization: String?,
    args: RemovePost,
  ): Result<PostResponse> =
    Result.failure(error)

  override suspend fun search(
    authorization: String?,
    args: Search,
    force: Boolean,
  ): Result<SearchResponse> =
    Result.failure(error)

  override suspend fun getSiteMetadata(
    authorization: String?,
    args: GetSiteMetadata,
    force: Boolean,
  ): Result<GetSiteMetadataResponse> =
    Result.failure(error)

  override suspend fun createCommentReport(
    authorization: String?,
    args: CreateCommentReport,
  ): Result<CommentReportResponse> =
    Result.failure(error)

  override suspend fun createPostReport(
    authorization: String?,
    args: CreatePostReport,
  ): Result<PostReportResponse> =
    Result.failure(error)

  override suspend fun blockPerson(
    authorization: String?,
    args: BlockPerson,
  ): Result<BlockPersonResponse> =
    Result.failure(error)

  override suspend fun blockCommunity(
    authorization: String?,
    args: BlockCommunity,
  ): Result<BlockCommunityResponse> =
    Result.failure(error)

  override suspend fun blockInstance(
    authorization: String?,
    args: BlockInstance,
  ): Result<BlockInstanceResponse> =
    Result.failure(error)

  override suspend fun saveUserSettings(
    authorization: String?,
    args: SaveUserSettings,
  ): Result<LoginResponse> =
    Result.failure(error)

  override suspend fun uploadImage(
    authorization: String?,
    url: String,
    fileName: String,
    imageIs: InputStream,
    mimeType: String?,
  ): Result<UploadImageResult> =
    Result.failure(error)

  override suspend fun resolveObject(
    authorization: String?,
    args: ResolveObject,
    force: Boolean,
  ): Result<ResolveObjectResponse> =
    Result.failure(error)

  override suspend fun banUserFromSite(
    authorization: String?,
    args: BanPerson,
  ): Result<BanPersonResponse> =
    Result.failure(error)

  override suspend fun removeCommunity(
    authorization: String?,
    args: RemoveCommunity,
  ): Result<CommunityResponse> =
    Result.failure(error)

  override suspend fun hideCommunity(
    authorization: String?,
    args: HideCommunity,
  ): Result<SuccessResponse> =
    Result.failure(error)

  override suspend fun purgePerson(
    authorization: String?,
    args: PurgePerson,
  ): Result<SuccessResponse> =
    Result.failure(error)

  override suspend fun purgeCommunity(
    authorization: String?,
    args: PurgeCommunity,
  ): Result<SuccessResponse> =
    Result.failure(error)

  override suspend fun purgePost(
    authorization: String?,
    args: PurgePost,
  ): Result<SuccessResponse> =
    Result.failure(error)

  override suspend fun purgeComment(
    authorization: String?,
    args: PurgeComment,
  ): Result<SuccessResponse> =
    Result.failure(error)

  override suspend fun getRegistrationApplicationsCount(
    authorization: String?,
    args: GetUnreadRegistrationApplicationCount,
    force: Boolean,
  ): Result<GetUnreadRegistrationApplicationCountResponse> =
    Result.failure(error)

  override suspend fun listRegistrationApplications(
    authorization: String?,
    args: ListRegistrationApplications,
    force: Boolean,
  ): Result<ListRegistrationApplicationsResponse> =
    Result.failure(error)

  override suspend fun approveRegistrationApplication(
    authorization: String?,
    args: ApproveRegistrationApplication,
  ): Result<RegistrationApplicationResponse> =
    Result.failure(error)

  override suspend fun getModLogs(
    authorization: String?,
    args: GetModlog,
    force: Boolean,
  ): Result<GetModlogResponse> =
    Result.failure(error)

  override suspend fun register(args: Register): Result<LoginResponse> =
    Result.failure(error)

  override suspend fun getCaptcha(): Result<GetCaptchaResponse> =
    Result.failure(error)

  override suspend fun listMedia(
    authorization: String?,
    args: ListMedia,
    force: Boolean,
  ): Result<ListMediaResponse> =
    Result.failure(error)

  override suspend fun deleteMedia(
    authorization: String?,
    args: DeleteImage,
  ): Result<Unit> =
    Result.failure(error)

  override fun supportsFeature(apiFeature: ApiFeature): Boolean =
    apiInfo.supportsFeature(apiFeature)
}