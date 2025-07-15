package com.idunnololz.summit.api

import android.util.Log
import arrow.core.Either
import coil3.util.MimeTypeMap
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.dto.lemmy.AddModToCommunity
import com.idunnololz.summit.api.dto.lemmy.AddModToCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.ApproveRegistrationApplication
import com.idunnololz.summit.api.dto.lemmy.BanFromCommunity
import com.idunnololz.summit.api.dto.lemmy.BanFromCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.BanPerson
import com.idunnololz.summit.api.dto.lemmy.BanPersonResponse
import com.idunnololz.summit.api.dto.lemmy.BlockCommunity
import com.idunnololz.summit.api.dto.lemmy.BlockInstance
import com.idunnololz.summit.api.dto.lemmy.BlockInstanceResponse
import com.idunnololz.summit.api.dto.lemmy.BlockPerson
import com.idunnololz.summit.api.dto.lemmy.ChangePassword
import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.api.dto.lemmy.CommentReplyId
import com.idunnololz.summit.api.dto.lemmy.CommentReplyView
import com.idunnololz.summit.api.dto.lemmy.CommentReportId
import com.idunnololz.summit.api.dto.lemmy.CommentReportResponse
import com.idunnololz.summit.api.dto.lemmy.CommentResponse
import com.idunnololz.summit.api.dto.lemmy.CommentSortType
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.CommunityId
import com.idunnololz.summit.api.dto.lemmy.CommunityResponse
import com.idunnololz.summit.api.dto.lemmy.CommunityView
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
import com.idunnololz.summit.api.dto.lemmy.GetCommunity
import com.idunnololz.summit.api.dto.lemmy.GetCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.GetModlog
import com.idunnololz.summit.api.dto.lemmy.GetModlogResponse
import com.idunnololz.summit.api.dto.lemmy.GetPersonDetails
import com.idunnololz.summit.api.dto.lemmy.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.lemmy.GetPersonMentions
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
import com.idunnololz.summit.api.dto.lemmy.GetSiteResponse
import com.idunnololz.summit.api.dto.lemmy.GetUnreadCount
import com.idunnololz.summit.api.dto.lemmy.GetUnreadCountResponse
import com.idunnololz.summit.api.dto.lemmy.GetUnreadRegistrationApplicationCount
import com.idunnololz.summit.api.dto.lemmy.GetUnreadRegistrationApplicationCountResponse
import com.idunnololz.summit.api.dto.lemmy.HideCommunity
import com.idunnololz.summit.api.dto.lemmy.InstanceId
import com.idunnololz.summit.api.dto.lemmy.ListCommentLikes
import com.idunnololz.summit.api.dto.lemmy.ListCommentLikesResponse
import com.idunnololz.summit.api.dto.lemmy.ListCommentReports
import com.idunnololz.summit.api.dto.lemmy.ListCommentReportsResponse
import com.idunnololz.summit.api.dto.lemmy.ListCommunities
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
import com.idunnololz.summit.api.dto.lemmy.ListingType
import com.idunnololz.summit.api.dto.lemmy.LockPost
import com.idunnololz.summit.api.dto.lemmy.Login
import com.idunnololz.summit.api.dto.lemmy.LoginResponse
import com.idunnololz.summit.api.dto.lemmy.MarkAllAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkCommentReplyAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkPersonMentionAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkPostAsRead
import com.idunnololz.summit.api.dto.lemmy.MarkPrivateMessageAsRead
import com.idunnololz.summit.api.dto.lemmy.ModlogActionType
import com.idunnololz.summit.api.dto.lemmy.PersonId
import com.idunnololz.summit.api.dto.lemmy.PersonMentionId
import com.idunnololz.summit.api.dto.lemmy.PersonMentionView
import com.idunnololz.summit.api.dto.lemmy.PersonView
import com.idunnololz.summit.api.dto.lemmy.PostFeatureType
import com.idunnololz.summit.api.dto.lemmy.PostId
import com.idunnololz.summit.api.dto.lemmy.PostReportId
import com.idunnololz.summit.api.dto.lemmy.PostReportResponse
import com.idunnololz.summit.api.dto.lemmy.PostView
import com.idunnololz.summit.api.dto.lemmy.PrivateMessageId
import com.idunnololz.summit.api.dto.lemmy.PrivateMessageReportResponse
import com.idunnololz.summit.api.dto.lemmy.PrivateMessageView
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
import com.idunnololz.summit.api.dto.lemmy.SortType
import com.idunnololz.summit.api.dto.lemmy.SuccessResponse
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.Consts
import com.idunnololz.summit.links.SiteBackendHelper
import com.idunnololz.summit.network.LemmyApi
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.extensionForMimeType
import com.idunnololz.summit.util.guessMimeType
import com.idunnololz.summit.util.retry
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import javax.inject.Singleton
import kotlin.math.max

const val COMMENTS_DEPTH_MAX = 6

class LemmyApiClient @Inject constructor(
  private val apiListenerManager: ApiListenerManager,
  private val preferences: Preferences,
  @LemmyApi val okHttpClient: OkHttpClient,
  private val siteBackendHelper: SiteBackendHelper,
  coroutineScopeFactory: CoroutineScopeFactory,
) {

  companion object {
    private const val TAG = "LemmyApiClient"

    private val apis = mutableMapOf<String, LemmyLikeApi>()
  }

  @Singleton
  class Factory @Inject constructor(
    private val apiListenerManager: ApiListenerManager,
    private val preferences: Preferences,
    @LemmyApi private val okHttpClient: OkHttpClient,
    private val siteBackendHelper: SiteBackendHelper,
    private val coroutineScopeFactory: CoroutineScopeFactory,
  ) {
    fun create() = LemmyApiClient(
      apiListenerManager = apiListenerManager,
      preferences = preferences,
      okHttpClient = okHttpClient,
      siteBackendHelper = siteBackendHelper,
      coroutineScopeFactory = coroutineScopeFactory,
    )
  }

  private val Account.bearer: String
    get() = "Bearer $jwt"

  private fun String.toBearer(): String = "Bearer $this"

  val instance: String
    get() = instanceFlow.value

  val instanceFlow = MutableStateFlow(preferences.guestAccountSettings?.instance
    ?: Consts.DEFAULT_INSTANCE)

  private val coroutineScope = coroutineScopeFactory.create()

  private val apiInfo =
    MutableStateFlow<StatefulData<SiteBackendHelper.ApiInfo>>(StatefulData.NotStarted())

  private var fetchApiInfoInstance: String? = null
  private var fetchApiInfoJob: Deferred<Result<SiteBackendHelper.ApiInfo>>? = null

  fun changeInstance(newInstance: String) {
    instanceFlow.value = newInstance
  }

  fun defaultInstance() {
    instanceFlow.value = preferences.guestAccountSettings?.instance
      ?: Consts.DEFAULT_INSTANCE
  }

  fun clearCache() {
    okHttpClient.cache?.evictAll()
  }

  suspend fun apiSupportsReports(): Boolean =
    getApi().apiSupportsReports

  suspend fun fetchPosts(
    account: Account?,
    communityIdOrName: Either<Int, String>? = null,
    sortType: SortType,
    listingType: ListingType,
    page: Int?,
    cursor: String?,
    limit: Int? = null,
    upvotedOnly: Boolean? = null,
    downvotedOnly: Boolean? = null,
    force: Boolean,
  ): Result<GetPostsResponse> {
    val communityId = communityIdOrName?.fold({ it }, { null })
    val communityName = communityIdOrName?.fold({ null }, { it })

    val form =
      GetPosts(
        community_id = communityId,
        community_name = communityName,
        sort = sortType,
        type_ = listingType,
        page = page,
        cursor = cursor,
        limit = limit,
        liked_only = upvotedOnly,
        disliked_only = downvotedOnly,
        auth = account?.jwt,
      )

    return onApiClient {
      getApi().getPosts(authorization = account?.bearer, args = form, force = force)
    }
  }

  suspend fun fetchPost(
    account: Account?,
    id: Either<PostId, CommentId>,
    force: Boolean,
  ): Result<GetPostResponse> {
    val postForm = id.fold({
      GetPost(id = it, auth = account?.jwt)
    }, {
      GetPost(comment_id = it, auth = account?.jwt)
    })

    return onApiClient {
      getApi().getPost(authorization = account?.bearer, args = postForm, force = force)
    }
  }

  suspend fun markPostAsRead(postId: PostId, read: Boolean, account: Account): Result<SuccessResponse> {
    val form = MarkPostAsRead(
      post_id = postId,
      post_ids = listOf(postId),
      read = read,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().markPostAsRead(authorization = account.bearer, args = form)
    }
  }

  suspend fun fetchSavedPosts(
    account: Account?,
    page: Int,
    limit: Int? = null,
    force: Boolean,
  ): Result<List<PostView>> {
    val form = GetPosts(
      community_id = null,
      community_name = null,
      sort = SortType.New,
      type_ = ListingType.All,
      page = page,
      limit = limit,
      saved_only = true,
      auth = account?.jwt,
    )

    return onApiClient {
      getApi().getPosts(authorization = account?.bearer, args = form, force = force)
    }.map { it.posts }
  }

  suspend fun fetchSavedComments(
    account: Account?,
    page: Int,
    limit: Int? = null,
    force: Boolean,
  ): Result<List<CommentView>> {
    val commentsForm =
      GetComments(
        // max_depth cannot be used right now due to a bug
        // See https://github.com/LemmyNet/lemmy/issues/3065
//                max_depth = 1,
        type_ = ListingType.All,
        post_id = null,
        sort = CommentSortType.New,
        saved_only = true,
        auth = account?.jwt,
        page = page,
        limit = limit,
      )

    return onApiClient {
      getApi().getComments(authorization = account?.bearer, commentsForm, force = force)
    }.map { it.comments }
  }

  suspend fun fetchComments(
    account: Account?,
    id: Either<PostId, CommentId>?,
    sort: CommentSortType,
    maxDepth: Int?,
    limit: Int? = null,
    page: Int? = null,
    upvotedOnly: Boolean? = null,
    downvotedOnly: Boolean? = null,
    force: Boolean = false,
  ): Result<List<CommentView>> {
    Log.d("HAHA", "maxDepth: ${maxDepth}", RuntimeException())
    val commentsForm = id?.fold({
      GetComments(
        max_depth = maxDepth,
        type_ = ListingType.All,
        post_id = it,
        sort = sort,
        auth = account?.jwt,
        limit = limit,
        page = page,
      )
    }, {
      GetComments(
        max_depth = maxDepth,
        type_ = ListingType.All,
        parent_id = it,
        sort = sort,
        auth = account?.jwt,
        limit = limit,
        page = page,
      )
    })
      ?: GetComments(
        max_depth = maxDepth,
        type_ = ListingType.All,
        post_id = null,
        sort = sort,
        auth = account?.jwt,
        liked_only = upvotedOnly,
        disliked_only = downvotedOnly,
        limit = limit,
        page = page,
      )

    return onApiClient {
      getApi().getComments(authorization = account?.bearer, commentsForm, force = force)
    }.map { it.comments }
  }

  suspend fun getCommunity(
    account: Account?,
    name: String,
    instance: String,
    force: Boolean,
  ): Result<CommunityView> {
    val finalName = if (instance == this.instance) {
      name
    } else {
      "$name@$instance"
    }

    return getCommunity(account, Either.Right(finalName), force)
      .map { it.community_view }
  }

  suspend fun getCommunity(
    account: Account?,
    idOrName: Either<Int, String>,
    force: Boolean,
  ): Result<GetCommunityResponse> {
    val form = idOrName.fold({ id ->
      GetCommunity(id = id, auth = account?.jwt)
    }, { name ->
      GetCommunity(name = name, auth = account?.jwt)
    })

    return onApiClient {
      getApi().getCommunity(authorization = account?.bearer, args = form, force = force)
    }
  }

  suspend fun createCommunity(
    account: Account?,
    createCommunity: CreateCommunity,
  ): Result<CommunityResponse> {
    return onApiClient {
      getApi().createCommunity(authorization = account?.bearer, args = createCommunity)
    }
  }

  suspend fun updateCommunity(
    account: Account?,
    editCommunity: EditCommunity,
  ): Result<CommunityResponse> {
    return onApiClient {
      getApi().updateCommunity(authorization = account?.bearer, args = editCommunity)
    }
  }

  suspend fun deleteCommunity(
    account: Account?,
    deleteCommunity: DeleteCommunity,
  ): Result<CommunityResponse> {
    return onApiClient {
      getApi().deleteCommunity(authorization = account?.bearer, args = deleteCommunity)
    }
  }

  suspend fun search(
    account: Account?,
    communityId: Int? = null,
    communityName: String? = null,
    sortType: SortType,
    listingType: ListingType,
    searchType: SearchType,
    page: Int? = null,
    query: String,
    limit: Int? = null,
    creatorId: Long? = null,
    force: Boolean,
  ): Result<SearchResponse> {
    val form = Search(
      q = query,
      type_ = searchType,
      creator_id = creatorId,
      community_id = communityId,
      community_name = communityName,
      sort = sortType,
      listing_type = listingType,
      page = page,
      limit = limit,
      auth = account?.jwt,
    )

    return onApiClient {
      getApi().search(authorization = account?.bearer, args = form, force = force)
    }
  }

  suspend fun fetchCommunities(
    account: Account?,
    sortType: SortType,
    listingType: ListingType,
    page: Int = 1,
    limit: Int = 50,
  ): Result<List<CommunityView>> {
    val form = ListCommunities(
      type_ = listingType,
      sort = sortType,
      page = page,
      limit = limit,
      auth = account?.jwt,
    )
    return onApiClient {
      getApi().getCommunityList(authorization = account?.bearer, args = form, force = false)
    }.map { it.communities }
  }

  suspend fun login(
    instance: String,
    username: String,
    password: String,
    twoFactorCode: String?,
  ): Result<String?> {
    val originalInstance = this.instance

    try {
      changeInstance(instance)
    } catch (e: Exception) {
      return Result.failure(e)
    }

    val form = Login(username, password, twoFactorCode)

    return onApiClient { getApi().login(args = form) }
      .fold(
        onSuccess = {
          Result.success(it.jwt)
        },
        onFailure = {
          changeInstance(originalInstance)
          Result.failure(it)
        },
      )
  }

  suspend fun fetchSiteWithRetry(auth: String?, force: Boolean): Result<GetSiteResponse> = retry {
    val form = GetSite(auth = auth)

    onApiClient {
      getApi().getSite(authorization = auth?.toBearer(), args = form, force = force)
    }
  }

  suspend fun fetchUnreadCount(force: Boolean, account: Account): Result<GetUnreadCountResponse> {
    val form = GetUnreadCount(account.jwt)

    return onApiClient {
      getApi().getUnreadCount(authorization = account.bearer, form, force = force)
    }
  }

  suspend fun fetchUnresolvedReportsCount(
    force: Boolean,
    account: Account,
  ): Result<GetReportCountResponse> {
    Log.d("HAHA", "fetchUnresolvedReportsCount()", RuntimeException())
    val form = GetReportCount(
      null,
      account.jwt,
    )

    return onApiClient {
      getApi().getReportCount(authorization = account.bearer, form, force = force)
    }
  }

  suspend fun likePostWithRetry(postId: Int, score: Int, account: Account): Result<PostView> =
    retry {
      val form = CreatePostLike(
        post_id = postId,
        score = score,
        auth = account.jwt,
      )

      onApiClient { getApi().likePost(authorization = account.bearer, form) }
        .map { it.post_view }
    }

  suspend fun likeCommentWithRetry(
    commentId: Int,
    score: Int,
    account: Account,
  ): Result<CommentView> = retry {
    val form = CreateCommentLike(
      comment_id = commentId,
      score = score,
      auth = account.jwt,
    )

    onApiClient { getApi().likeComment(authorization = account.bearer, form) }
      .map { it.comment_view }
  }

  suspend fun listCommentVotesWithRetry(
    commentId: Int,
    page: Long = 1,
    limit: Long = 20,
    force: Boolean = false,
    account: Account,
  ): Result<ListCommentLikesResponse> = retry {
    val form = ListCommentLikes(
      comment_id = commentId,
      page = page,
      limit = limit,
    )

    onApiClient {
      getApi().listCommentVotes(authorization = account.bearer, form, force = force)
    }
  }

  suspend fun listPostVotesWithRetry(
    postId: Int,
    page: Long = 1,
    limit: Long = 20,
    force: Boolean = false,
    account: Account,
  ): Result<ListPostLikesResponse> = retry {
    val form = ListPostLikes(
      post_id = postId,
      page = page,
      limit = limit,
    )

    onApiClient {
      getApi().listPostVotes(authorization = account.bearer, form, force = force)
    }
  }

  suspend fun followCommunityWithRetry(
    communityId: Int,
    follow: Boolean,
    account: Account,
  ): Result<CommunityView> = retry {
    val form = FollowCommunity(
      community_id = communityId,
      follow = follow,
      auth = account.jwt,
    )

    onApiClient { getApi().followCommunity(authorization = account.bearer, form) }
      .map { it.community_view }
  }

  suspend fun banUserFromCommunity(
    communityId: CommunityId,
    personId: PersonId,
    ban: Boolean,
    removeData: Boolean,
    reason: String?,
    expiresTs: Long?,
    account: Account,
  ): Result<BanFromCommunityResponse> {
    val form = BanFromCommunity(
      community_id = communityId,
      person_id = personId,
      ban = ban,
      remove_data = removeData,
      reason = reason,
      expires = expiresTs,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().banUserFromCommunity(authorization = account.bearer, form)
    }
  }

  suspend fun modUser(
    communityId: CommunityId,
    personId: PersonId,
    add: Boolean,
    account: Account,
  ): Result<AddModToCommunityResponse> {
    val form = AddModToCommunity(
      communityId,
      personId,
      add,
      account.jwt,
    )

    return onApiClient {
      getApi().modUser(authorization = account.bearer, form)
    }
  }

  suspend fun distinguishComment(
    commentId: CommentId,
    distinguish: Boolean,
    account: Account,
  ): Result<CommentResponse> {
    val form = DistinguishComment(
      commentId,
      distinguish,
      account.jwt,
    )

    return onApiClient {
      getApi().distinguishComment(authorization = account.bearer, args = form)
    }
  }

  suspend fun removeComment(
    commentId: CommentId,
    remove: Boolean,
    reason: String?,
    account: Account,
  ): Result<CommentResponse> {
    val form = RemoveComment(
      commentId,
      remove,
      reason,
      account.jwt,
    )

    return onApiClient {
      getApi().removeComment(authorization = account.bearer, form)
    }
  }

  suspend fun createComment(
    account: Account,
    content: String,
    postId: PostId,
    parentId: CommentId?,
    languageId: Int?,
  ): Result<CommentView> {
    val form = CreateComment(
      auth = account.jwt,
      content = content,
      post_id = postId,
      parent_id = parentId,
      language_id = languageId,
    )

    return onApiClient {
      getApi().createComment(authorization = account.bearer, form)
    }.map { it.comment_view }
  }

  suspend fun editComment(
    account: Account,
    content: String,
    commentId: CommentId,
    languageId: Int?,
  ): Result<CommentView> {
    val form = EditComment(
      auth = account.jwt,
      content = content,
      comment_id = commentId,
      language_id = languageId,
    )

    return onApiClient {
      getApi().editComment(authorization = account.bearer, form)
    }.map { it.comment_view }
  }

  suspend fun deleteComment(account: Account, commentId: CommentId): Result<CommentView> {
    val form = DeleteComment(
      auth = account.jwt,
      comment_id = commentId,
      deleted = true,
    )

    return onApiClient {
      getApi().deleteComment(authorization = account.bearer, form)
    }.map { it.comment_view }
  }

  suspend fun createPost(
    name: String,
    body: String?,
    url: String?,
    isNsfw: Boolean,
    thumbnailUrl: String?,
    altText: String?,
    languageId: Int?,
    account: Account,
    communityId: CommunityId,
  ): Result<PostView> {
    val form = CreatePost(
      name = name,
      community_id = communityId,
      url = url,
      body = body,
      nsfw = isNsfw,
      auth = account.jwt,
      custom_thumbnail = thumbnailUrl,
      alt_text = altText,
      language_id = languageId,
    )

    return onApiClient {
      getApi().createPost(authorization = account.bearer, form)
    }.map { it.post_view }
  }

  suspend fun editPost(
    postId: PostId,
    name: String,
    body: String?,
    url: String?,
    isNsfw: Boolean,
    thumbnailUrl: String?,
    altText: String?,
    languageId: Int?,
    account: Account,
  ): Result<PostView> {
    val form = EditPost(
      post_id = postId,
      name = name,
      url = url,
      body = body,
      nsfw = isNsfw,
      auth = account.jwt,
      custom_thumbnail = thumbnailUrl,
      alt_text = altText,
      language_id = languageId,
    )

    return onApiClient {
      getApi().editPost(authorization = account.bearer, form)
    }.map { it.post_view }
  }

  suspend fun deletePost(account: Account, id: PostId, delete: Boolean): Result<PostView> {
    val form = DeletePost(
      post_id = id,
      deleted = delete,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().deletePost(authorization = account.bearer, form)
    }.map { it.post_view }
  }

  suspend fun featurePost(
    account: Account,
    id: PostId,
    featured: Boolean,
    featureType: PostFeatureType,
  ): Result<PostView> {
    val form = FeaturePost(
      post_id = id,
      featured = featured,
      feature_type = featureType,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().featurePost(authorization = account.bearer, form)
    }.map { it.post_view }
  }

  suspend fun lockPost(account: Account, id: PostId, locked: Boolean): Result<PostView> {
    val form = LockPost(
      post_id = id,
      locked = locked,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().lockPost(authorization = account.bearer, form)
    }.map { it.post_view }
  }

  suspend fun removePost(
    account: Account,
    id: PostId,
    reason: String?,
    removed: Boolean,
  ): Result<PostView> {
    val form = RemovePost(
      post_id = id,
      removed = removed,
      reason = reason,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().removePost(authorization = account.bearer, form)
    }.map { it.post_view }
  }

  suspend fun uploadImage(
    account: Account,
    fileName: String,
    imageIs: InputStream,
  ): Result<UploadImageResult> {
    val url = "https://${instance}/pictrs/image"
    val mimeType = guessMimeType(imageIs)
    val fileNameWithExtension = if (mimeType == null || fileName.contains(".")) {
      fileName
    } else {
      val extension = extensionForMimeType(mimeType)
      if (extension != null) {
        "$fileName.$extension"
      } else {
        fileName
      }
    }

    return onApiClient {
      getApi().uploadImage(
        authorization = account.bearer,
        url = url,
        fileName = fileNameWithExtension,
        imageIs = imageIs,
        mimeType = mimeType,
      )
    }
  }

  suspend fun blockCommunity(
    communityId: CommunityId,
    block: Boolean,
    account: Account,
  ): Result<CommunityView> {
    val form = BlockCommunity(
      community_id = communityId,
      block = block,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().blockCommunity(authorization = account.bearer, form)
    }.map { it.community_view }
  }

  suspend fun blockPerson(
    personId: PersonId,
    block: Boolean,
    account: Account,
  ): Result<PersonView> {
    val form = BlockPerson(
      person_id = personId,
      block = block,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().blockPerson(authorization = account.bearer, form)
    }.map { it.person_view }
  }

  suspend fun blockInstance(
    instanceId: InstanceId,
    block: Boolean,
    account: Account,
  ): Result<BlockInstanceResponse> {
    val form = BlockInstance(
      instance_id = instanceId,
      block = block,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().blockInstance(authorization = account.bearer, form)
    }
  }

  suspend fun fetchPerson(
    personId: PersonId?,
    name: String?,
    sort: SortType = SortType.New,
    page: Int? = null,
    limit: Int? = null,
    account: Account?,
    force: Boolean,
    savedOnly: Boolean = false,
    includeContent: Boolean? = null,
  ): Result<GetPersonDetailsResponse> {
    val form = GetPersonDetails(
      person_id = personId,
      username = name,
      auth = account?.jwt,
      page = page,
      limit = limit,
      sort = sort,
      saved_only = savedOnly,
      include_content = includeContent,
    )

    return onApiClient {
      getApi().getPersonDetails(authorization = account?.bearer, form, force = force)
    }
  }

  suspend fun changePassword(
    newPassword: String,
    newPasswordVerify: String,
    oldPassword: String,
    account: Account,
  ): Result<LoginResponse> {
    val changePassword = ChangePassword(
      new_password = newPassword,
      new_password_verify = newPasswordVerify,
      old_password = oldPassword,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().changePassword(authorization = account.bearer, changePassword)
    }
  }

  suspend fun fetchReplies(
    sort: CommentSortType? /* "Hot" | "Top" | "New" | "Old" */ = null,
    page: Int? = null,
    limit: Int? = null,
    unreadOnly: Boolean? = null,
    account: Account,
    force: Boolean,
  ): Result<List<CommentReplyView>> {
    val form = GetReplies(
      sort = sort,
      page = page,
      limit = limit,
      unread_only = unreadOnly,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().getReplies(authorization = account.bearer, form, force = force)
    }.map { it.replies }
  }

  suspend fun fetchMentions(
    sort: CommentSortType? /* "Hot" | "Top" | "New" | "Old" */ = null,
    page: Int? = null,
    limit: Int? = null,
    unreadOnly: Boolean? = null,
    account: Account,
    force: Boolean,
  ): Result<List<PersonMentionView>> {
    val form = GetPersonMentions(
      sort,
      page,
      limit,
      unreadOnly,
      account.jwt,
    )

    return onApiClient {
      getApi().getPersonMentions(authorization = account.bearer, form, force = force)
    }.map { it.mentions }
  }

  suspend fun fetchPrivateMessages(
    unreadOnly: Boolean? = null,
    page: Int? = null,
    limit: Int? = null,
    senderId: PersonId? = null,
    account: Account,
    force: Boolean,
  ): Result<List<PrivateMessageView>> {
    val form = GetPrivateMessages(
      unread_only = unreadOnly,
      page = page,
      limit = limit,
      creator_id = senderId,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().getPrivateMessages(authorization = account.bearer, form, force = force)
    }.map { it.private_messages }
  }

  suspend fun fetchPrivateMessageReports(
    unresolvedOnly: Boolean? = null,
    page: Int? = null,
    limit: Int? = null,
    account: Account,
    force: Boolean,
  ): Result<ListPrivateMessageReportsResponse> {
    val form = ListPrivateMessageReports(
      page,
      limit,
      unresolvedOnly,
      account.jwt,
    )

    return onApiClient {
      getApi().getPrivateMessageReports(authorization = account.bearer, form, force = force)
    }
  }

  suspend fun fetchPostReports(
    unresolvedOnly: Boolean? = null,
    page: Int? = null,
    limit: Int? = null,
    account: Account,
    force: Boolean,
  ): Result<ListPostReportsResponse> {
    val form = ListPostReports(
      page = page,
      limit = limit,
      unresolved_only = unresolvedOnly,
      community_id = null,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().getPostReports(authorization = account.bearer, form, force = force)
    }
  }

  suspend fun createPrivateMessageReport(
    privateMessageId: Int,
    reason: String,
    account: Account,
  ): Result<PrivateMessageReportResponse> {
    val form = CreatePrivateMessageReport(
      auth = account.jwt,
      private_message_id = privateMessageId,
      reason = reason,
    )

    return onApiClient {
      getApi().createPrivateMessageReport(authorization = account.bearer, form)
    }
  }

  suspend fun resolvePrivateMessageReport(
    reportId: Int,
    resolved: Boolean,
    account: Account,
  ): Result<PrivateMessageReportResponse> {
    val form = ResolvePrivateMessageReport(
      auth = account.jwt,
      report_id = reportId,
      resolved = resolved,
    )

    return onApiClient {
      getApi().resolvePrivateMessageReport(authorization = account.bearer, form)
    }
  }

  suspend fun resolvePostReport(
    reportId: PostReportId,
    resolved: Boolean,
    account: Account,
  ): Result<PostReportResponse> {
    val form = ResolvePostReport(
      auth = account.jwt,
      report_id = reportId,
      resolved = resolved,
    )

    return onApiClient {
      getApi().resolvePostReport(authorization = account.bearer, form)
    }
  }

  suspend fun fetchCommentReports(
    unresolvedOnly: Boolean? = null,
    page: Int? = null,
    limit: Int? = null,
    account: Account,
    force: Boolean,
  ): Result<ListCommentReportsResponse> {
    val form = ListCommentReports(
      page = page,
      limit = limit,
      unresolved_only = unresolvedOnly,
      community_id = null,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().getCommentReports(authorization = account.bearer, form, force = force)
    }
  }

  suspend fun resolveCommentReport(
    reportId: CommentReportId,
    resolved: Boolean,
    account: Account,
  ): Result<CommentReportResponse> {
    val form = ResolveCommentReport(
      auth = account.jwt,
      report_id = reportId,
      resolved = resolved,
    )

    return onApiClient {
      getApi().resolveCommentReport(authorization = account.bearer, form)
    }
  }

  suspend fun markReplyAsRead(
    id: CommentReplyId,
    read: Boolean,
    account: Account,
  ): Result<CommentView> {
    val form = MarkCommentReplyAsRead(
      id,
      read,
      account.jwt,
    )
    return onApiClient {
      getApi().markCommentReplyAsRead(authorization = account.bearer, form)
    }.map { it.comment_view }
  }

  suspend fun markMentionAsRead(
    id: PersonMentionId,
    read: Boolean,
    account: Account,
  ): Result<PersonMentionView> {
    val form = MarkPersonMentionAsRead(
      id,
      read,
      account.jwt,
    )
    return onApiClient {
      getApi().markPersonMentionAsRead(authorization = account.bearer, form)
    }.map { it.person_mention_view }
  }

  suspend fun markPrivateMessageAsRead(
    id: PrivateMessageId,
    read: Boolean,
    account: Account,
  ): Result<PrivateMessageView> {
    val form = MarkPrivateMessageAsRead(
      id,
      read,
      account.jwt,
    )
    return onApiClient {
      getApi().markPrivateMessageAsRead(authorization = account.bearer, form)
    }.map { it.private_message_view }
  }

  suspend fun markAllAsRead(account: Account): Result<GetRepliesResponse> {
    val form = MarkAllAsRead(
      account.jwt,
    )
    return onApiClient {
      getApi().markAllAsRead(authorization = account.bearer, form)
    }
  }

  suspend fun createPrivateMessage(
    content: String,
    recipient: PersonId,
    account: Account,
  ): Result<PrivateMessageView> {
    val form = CreatePrivateMessage(
      content = content,
      recipient_id = recipient,
      auth = account.jwt,
    )
    return onApiClient {
      getApi().createPrivateMessage(authorization = account.bearer, form)
    }.map { it.private_message_view }
  }

  suspend fun savePost(postId: PostId, save: Boolean, account: Account): Result<PostView> {
    val form = SavePost(postId, save, account.jwt)

    return onApiClient {
      getApi().savePost(authorization = account.bearer, form)
    }.map { it.post_view }
  }

  suspend fun saveComment(
    commentId: CommentId,
    save: Boolean,
    account: Account,
  ): Result<CommentView> {
    val form = SaveComment(commentId, save, account.jwt)

    return onApiClient {
      getApi().saveComment(authorization = account.bearer, form)
    }.map { it.comment_view }
  }

  suspend fun createPostReport(
    postId: PostId,
    reason: String,
    account: Account,
  ): Result<PostReportResponse> {
    val form = CreatePostReport(
      postId,
      reason,
      account.jwt,
    )

    return onApiClient {
      getApi().createPostReport(authorization = account.bearer, form)
    }
  }

  suspend fun createCommentReport(
    commentId: CommentId,
    reason: String,
    account: Account,
  ): Result<CommentReportResponse> {
    val form = CreateCommentReport(
      commentId,
      reason,
      account.jwt,
    )

    return onApiClient {
      getApi().createCommentReport(authorization = account.bearer, form)
    }
  }

  suspend fun saveUserSettings(settings: SaveUserSettings): Result<Unit> {
    return onApiClient {
      getApi().saveUserSettings(authorization = settings.auth.toBearer(), settings)
    }.map { Unit }
  }

  suspend fun resolveObject(q: String, account: Account): Result<ResolveObjectResponse> {
    val form = ResolveObject(
      q = q,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().resolveObject(authorization = account.bearer, form, force = false)
    }
  }

  suspend fun banUserFromSite(
    personId: PersonId,
    ban: Boolean,
    removeData: Boolean,
    reason: String?,
    expiresTs: Long?,
    account: Account,
  ): Result<BanPersonResponse> {
    val form = BanPerson(
      person_id = personId,
      ban = ban,
      remove_data = removeData,
      reason = reason,
      expires = expiresTs,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().banUserFromSite(authorization = account.bearer, form)
    }
  }

  suspend fun removeCommunity(
    communityId: CommunityId,
    remove: Boolean,
    reason: String?,
    account: Account,
  ): Result<CommunityResponse> {
    val form = RemoveCommunity(
      community_id = communityId,
      removed = remove,
      reason = reason,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().removeCommunity(authorization = account.bearer, form)
    }
  }

  suspend fun hideCommunity(
    communityId: CommunityId,
    hide: Boolean,
    reason: String?,
    account: Account,
  ): Result<SuccessResponse> {
    val form = HideCommunity(
      community_id = communityId,
      hidden = hide,
      reason = reason,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().hideCommunity(authorization = account.bearer, form)
    }
  }

  suspend fun purgePerson(
    personId: PersonId,
    reason: String?,
    account: Account,
  ): Result<SuccessResponse> {
    val form = PurgePerson(
      person_id = personId,
      reason = reason,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().purgePerson(authorization = account.bearer, form)
    }
  }

  suspend fun purgeCommunity(
    communityId: CommunityId,
    reason: String?,
    account: Account,
  ): Result<SuccessResponse> {
    val form = PurgeCommunity(
      community_id = communityId,
      reason = reason,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().purgeCommunity(authorization = account.bearer, form)
    }
  }

  suspend fun purgePost(
    postId: PostId,
    reason: String?,
    account: Account,
  ): Result<SuccessResponse> {
    val form = PurgePost(
      post_id = postId,
      reason = reason,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().purgePost(authorization = account.bearer, form)
    }
  }

  suspend fun purgeComment(
    commentId: CommentId,
    reason: String?,
    account: Account,
  ): Result<SuccessResponse> {
    val form = PurgeComment(
      comment_id = commentId,
      reason = reason,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().purgeComment(authorization = account.bearer, form)
    }
  }

  /**
   * @param modPersonId the id of the mod
   * @param otherPersonId the id of the person that the action was against
   */
  suspend fun fetchModLogs(
    modPersonId: PersonId? = null,
    communityId: CommunityId? = null,
    page: Int? = null,
    limit: Int? = null,
    actionType: ModlogActionType? /* "All" | "ModRemovePost" | "ModLockPost" | "ModFeaturePost" | "ModRemoveComment" | "ModRemoveCommunity" | "ModBanFromCommunity" | "ModAddCommunity" | "ModTransferCommunity" | "ModAdd" | "ModBan" | "ModHideCommunity" | "AdminPurgePerson" | "AdminPurgeCommunity" | "AdminPurgePost" | "AdminPurgeComment" */ =
      null,
    otherPersonId: PersonId? = null,
    post_id: PostId? = null,
    comment_id: CommentId? = null,
    account: Account? = null,
    force: Boolean,
  ): Result<GetModlogResponse> {
    val form = GetModlog(
      mod_person_id = modPersonId,
      community_id = communityId,
      page = page,
      limit = limit,
      type_ = actionType,
      other_person_id = otherPersonId,
      post_id = post_id,
      comment_id = comment_id,
      auth = account?.jwt,
    )

    return onApiClient {
      getApi().getModLogs(authorization = account?.bearer, form, force = force)
    }
  }

  suspend fun getUnreadRegistrationApplicationsCount(
    account: Account,
    force: Boolean,
  ): Result<GetUnreadRegistrationApplicationCountResponse> {
    val form = GetUnreadRegistrationApplicationCount(
      auth = account.jwt,
    )

    return onApiClient {
      getApi().getRegistrationApplicationsCount(authorization = account.bearer, form, force = force)
    }
  }

  suspend fun getRegistrationApplications(
    page: Int? = null,
    limit: Int? = null,
    unreadOnly: Boolean? = null,
    account: Account,
    force: Boolean,
  ): Result<ListRegistrationApplicationsResponse> {
    val form = ListRegistrationApplications(
      page = page,
      limit = limit,
      unread_only = unreadOnly,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().listRegistrationApplications(authorization = account.bearer, form, force = force)
    }
  }

  suspend fun approveRegistrationApplication(
    applicationId: Int,
    approve: Boolean,
    denyReason: String?,
    account: Account,
  ): Result<RegistrationApplicationResponse> {
    val form = ApproveRegistrationApplication(
      id = applicationId,
      approve = approve,
      deny_reason = denyReason,
      auth = account.jwt,
    )

    return onApiClient {
      getApi().approveRegistrationApplication(authorization = account.bearer, form)
    }
  }

  suspend fun listMedia(page: Long?, limit: Long?, account: Account, force: Boolean): Result<ListMediaResponse> {
    val form = ListMedia(
      page = page,
      limit = limit,
    )

    return onApiClient {
      getApi().listMedia(authorization = account.bearer, form, force = force)
    }
  }

  suspend fun register(
    username: String,
    password: String,
    passwordVerify: String,
    showNsfw: Boolean,
    /**
     * email is mandatory if email verification is enabled on the server
     */
    email: String? = null,
    /**
     * The UUID of the captcha item.
     */
    captchaUuid: String? = null,
    /**
     * Your captcha answer.
     */
    captchaAnswer: String? = null,
    /**
     * A form field to trick signup bots. Should be None.
     */
    honeypot: String? = null,
    /**
     * An answer is mandatory if require application is enabled on the server
     */
    answer: String? = null,
  ): Result<LoginResponse> {
    val form = Register(
      username = username,
      password = password,
      password_verify = passwordVerify,
      show_nsfw = showNsfw,
      email = email,
      captcha_uuid = captchaUuid,
      captcha_answer = captchaAnswer,
      honeypot = honeypot,
      answer = answer,
    )

    return onApiClient {
      getApi().register(form)
    }
  }

  suspend fun getCaptcha(): Result<GetCaptchaResponse> {
    return onApiClient {
      getApi().getCaptcha()
    }
  }

  private suspend fun <T> onApiClient(call: suspend () -> Result<T>): Result<T> {
    try {
      val result = call()
      apiListenerManager.onRequestComplete(result)
      return result
    } catch (e: Exception) {
      // This can happen if we are using the API is created by reflection.
      return Result.failure(e)
    }
  }

  private suspend fun getApi(instance: String = this.instance): LemmyLikeApi {
    val cachedApi = apis[instance]
    if (cachedApi != null) {
      return cachedApi
    }
    apiInfo.value = StatefulData.Loading()

    val currentFetchApiInfoJob = fetchApiInfoJob
    val fetchApiInfoJob = if (fetchApiInfoInstance == instance && currentFetchApiInfoJob != null) {
      currentFetchApiInfoJob
    } else {
      fetchApiInfoInstance = instance
      fetchApiInfoJob?.cancel()
      coroutineScope.async {
        siteBackendHelper.fetchApiInfo(instance)
      }.also {
        fetchApiInfoJob = it
      }
    }

    val result = fetchApiInfoJob.await()

    apiInfo.value = result.fold(
      {
        Log.d(TAG, "Determined backend for instance $instance is ${it.backendType}.")
        StatefulData.Success(it)
      },
      {
        Log.e(TAG, "Unable to determine backend for instance $instance.")
        StatefulData.Error(it)
      }
    )

    return newApi(instance, result).also {
      apis[instance] = it
    }
  }

  private fun newApi(
    instance: String = Consts.DEFAULT_INSTANCE,
    apiInfo: Result<SiteBackendHelper.ApiInfo>,
  ): LemmyLikeApi {
    return apiInfo.fold(
      {
        when (it.backendType) {
          SiteBackendHelper.ApiType.PieFedAlpha -> PieFedApiAlphaAdapter(
            Retrofit.Builder()
              .baseUrl("https://$instance/api/alpha/")
              .addConverterFactory(GsonConverterFactory.create())
              .client(okHttpClient)
              .build()
              .create(PieFedApiAlpha::class.java),
            instance,
          )
          SiteBackendHelper.ApiType.LemmyV4,
          SiteBackendHelper.ApiType.LemmyV3,
          null -> LemmyApiV3Adapter(
            Retrofit.Builder()
              .baseUrl("https://$instance/api/v3/")
              .addConverterFactory(GsonConverterFactory.create())
              .client(okHttpClient)
              .build()
              .create(LemmyApiV3::class.java),
            instance,
          )
        }
      },
      { exception ->
        val handler = object : InvocationHandler {
          override fun invoke(
            proxy: Any?,
            method: Method?,
            args: Array<out Any?>?,
          ): Any? {
            throw exception
          }
        }
        Proxy.newProxyInstance(
          LemmyLikeApi::class.java.classLoader,
          arrayOf(LemmyLikeApi::class.java),
          handler,
        ) as LemmyLikeApi
      }
    )
  }
}