package com.idunnololz.summit.api

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.dto.AddModToCommunity
import com.idunnololz.summit.api.dto.AddModToCommunityResponse
import com.idunnololz.summit.api.dto.ApproveRegistrationApplication
import com.idunnololz.summit.api.dto.BanFromCommunity
import com.idunnololz.summit.api.dto.BanFromCommunityResponse
import com.idunnololz.summit.api.dto.BanPerson
import com.idunnololz.summit.api.dto.BanPersonResponse
import com.idunnololz.summit.api.dto.BlockCommunity
import com.idunnololz.summit.api.dto.BlockInstance
import com.idunnololz.summit.api.dto.BlockInstanceResponse
import com.idunnololz.summit.api.dto.BlockPerson
import com.idunnololz.summit.api.dto.ChangePassword
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentReplyId
import com.idunnololz.summit.api.dto.CommentReplyView
import com.idunnololz.summit.api.dto.CommentReportId
import com.idunnololz.summit.api.dto.CommentReportResponse
import com.idunnololz.summit.api.dto.CommentResponse
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.CommunityResponse
import com.idunnololz.summit.api.dto.CommunityView
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
import com.idunnololz.summit.api.dto.GetCommunity
import com.idunnololz.summit.api.dto.GetCommunityResponse
import com.idunnololz.summit.api.dto.GetModlog
import com.idunnololz.summit.api.dto.GetModlogResponse
import com.idunnololz.summit.api.dto.GetPersonDetails
import com.idunnololz.summit.api.dto.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.GetPersonMentions
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
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.GetUnreadCount
import com.idunnololz.summit.api.dto.GetUnreadCountResponse
import com.idunnololz.summit.api.dto.GetUnreadRegistrationApplicationCount
import com.idunnololz.summit.api.dto.GetUnreadRegistrationApplicationCountResponse
import com.idunnololz.summit.api.dto.HideCommunity
import com.idunnololz.summit.api.dto.InstanceId
import com.idunnololz.summit.api.dto.ListCommentLikes
import com.idunnololz.summit.api.dto.ListCommentLikesResponse
import com.idunnololz.summit.api.dto.ListCommentReports
import com.idunnololz.summit.api.dto.ListCommentReportsResponse
import com.idunnololz.summit.api.dto.ListCommunities
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
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.LockPost
import com.idunnololz.summit.api.dto.Login
import com.idunnololz.summit.api.dto.LoginResponse
import com.idunnololz.summit.api.dto.MarkAllAsRead
import com.idunnololz.summit.api.dto.MarkCommentReplyAsRead
import com.idunnololz.summit.api.dto.MarkPersonMentionAsRead
import com.idunnololz.summit.api.dto.MarkPostAsRead
import com.idunnololz.summit.api.dto.MarkPrivateMessageAsRead
import com.idunnololz.summit.api.dto.ModlogActionType
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PersonMentionId
import com.idunnololz.summit.api.dto.PersonMentionView
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.dto.PostFeatureType
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostReportId
import com.idunnololz.summit.api.dto.PostReportResponse
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.PrivateMessageId
import com.idunnololz.summit.api.dto.PrivateMessageReportResponse
import com.idunnololz.summit.api.dto.PrivateMessageView
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
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.api.dto.SuccessResponse
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.Consts
import com.idunnololz.summit.links.SiteBackendHelper
import com.idunnololz.summit.network.LemmyApi
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils.serializeToMap
import com.idunnololz.summit.util.retry
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

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

  private var api: LemmyLikeApi = getApiV3WithInstance(
    instance = preferences.guestAccountSettings?.instance
      ?: Consts.DEFAULT_INSTANCE,
  )

  private val Account.bearer: String
    get() = "Bearer $jwt"

  private fun String.toBearer(): String = "Bearer $this"

  val instance: String
    get() = instanceFlow.value

  val instanceFlow = MutableStateFlow(api.instance)

  private val coroutineScope = coroutineScopeFactory.create()

  private val apiInfo =
    MutableStateFlow<StatefulData<SiteBackendHelper.ApiInfo>>(StatefulData.NotStarted())

  init {
    var fetchApiInfoJob: Job? = null

    coroutineScope.launch {
      instanceFlow.collect {
        apiInfo.value = StatefulData.Loading()

        fetchApiInfoJob?.cancel()
        fetchApiInfoJob = coroutineScope.launch {
          apiInfo.value = siteBackendHelper.fetchApiInfo(it).fold(
            {
              Log.d(TAG, "Determined backend for instance $instance is ${it.backendType}.")
              StatefulData.Success(it)
            },
            { StatefulData.Error(it) }
          )
        }
      }
    }

    coroutineScope.launch {
      apiInfo.collect {

      }
    }
  }

  fun changeInstance(newInstance: String) {
    try {
      api = getApiV3WithInstance(instance = newInstance)

      instanceFlow.value = api.instance
    } catch (e: Exception) {
      Log.w(TAG, "Invalid instance: $newInstance", e)
    }
  }

  fun refreshClient() {
    api = getApiV3WithInstance(instance = instance)
  }

  fun defaultInstance() {
    api = getApiV3WithInstance(
      instance = preferences.guestAccountSettings?.instance
        ?: Consts.DEFAULT_INSTANCE,
    )

    instanceFlow.value = api.instance
  }

  fun clearCache() {
    okHttpClient.cache?.evictAll()
  }

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

    return retrofitErrorHandler {
      api.getPosts(authorization = account?.bearer, args = form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.getPost(authorization = account?.bearer, args = postForm, force = force)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun markPostAsRead(postId: PostId, read: Boolean, account: Account): Result<PostView> {
    val form = MarkPostAsRead(
      post_id = postId,
      post_ids = listOf(postId),
      read = read,
      auth = account.jwt,
    )

    return retrofitErrorHandler {
      api.markPostAsRead(authorization = account.bearer, args = form)
    }.fold(
      onSuccess = {
        Result.success(it.post_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.getPosts(authorization = account?.bearer, args = form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it.posts)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.getComments(authorization = account?.bearer, commentsForm, force = force)
    }.fold(
      onSuccess = {
        Result.success(it.comments)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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
    val commentsForm = id?.fold({
      GetComments(
        max_depth = maxDepth ?: COMMENTS_DEPTH_MAX,
        type_ = ListingType.All,
        post_id = it,
        sort = sort,
        auth = account?.jwt,
        limit = limit,
        page = page,
      )
    }, {
      GetComments(
        max_depth = maxDepth ?: COMMENTS_DEPTH_MAX,
        type_ = ListingType.All,
        parent_id = it,
        sort = sort,
        auth = account?.jwt,
        limit = limit,
        page = page,
      )
    })
      ?: GetComments(
        max_depth = maxDepth ?: COMMENTS_DEPTH_MAX,
        type_ = ListingType.All,
        post_id = null,
        sort = sort,
        auth = account?.jwt,
        liked_only = upvotedOnly,
        disliked_only = downvotedOnly,
        limit = limit,
        page = page,
      )

    return retrofitErrorHandler {
      api.getComments(authorization = account?.bearer, commentsForm, force = force)
    }.fold(
      onSuccess = {
        Result.success(it.comments)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.getCommunity(authorization = account?.bearer, args = form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun createCommunity(
    account: Account?,
    createCommunity: CreateCommunity,
  ): Result<CommunityResponse> {
    return retrofitErrorHandler {
      api.createCommunity(authorization = account?.bearer, args = createCommunity)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun updateCommunity(
    account: Account?,
    editCommunity: EditCommunity,
  ): Result<CommunityResponse> {
    return retrofitErrorHandler {
      api.updateCommunity(authorization = account?.bearer, args = editCommunity)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun deleteCommunity(
    account: Account?,
    deleteCommunity: DeleteCommunity,
  ): Result<CommunityResponse> {
    return retrofitErrorHandler {
      api.deleteCommunity(authorization = account?.bearer, args = deleteCommunity)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.search(authorization = account?.bearer, args = form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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
    return retrofitErrorHandler {
      api.getCommunityList(authorization = account?.bearer, args = form, force = false)
    }.fold(
      onSuccess = {
        Result.success(it.communities)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler { api.login(args = form) }
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

    retrofitErrorHandler {
      api.getSite(authorization = auth?.toBearer(), args = form, force = force)
    }
      .fold(
        onSuccess = {
          Result.success(it)
        },
        onFailure = {
          Result.failure(it)
        },
      )
  }

  suspend fun fetchUnreadCount(force: Boolean, account: Account): Result<GetUnreadCountResponse> {
    val form = GetUnreadCount(account.jwt)

    return retrofitErrorHandler {
      api.getUnreadCount(authorization = account.bearer, form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun fetchUnresolvedReportsCount(
    force: Boolean,
    account: Account,
  ): Result<GetReportCountResponse> {
    val form = GetReportCount(
      null,
      account.jwt,
    )

    return retrofitErrorHandler {
      api.getReportCount(authorization = account.bearer, form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun likePostWithRetry(postId: Int, score: Int, account: Account): Result<PostView> =
    retry {
      val form = CreatePostLike(
        post_id = postId,
        score = score,
        auth = account.jwt,
      )

      retrofitErrorHandler { api.likePost(authorization = account.bearer, form) }
        .fold(
          onSuccess = {
            Result.success(it.post_view)
          },
          onFailure = {
            Result.failure(it)
          },
        )
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

    retrofitErrorHandler { api.likeComment(authorization = account.bearer, form) }
      .fold(
        onSuccess = {
          Result.success(it.comment_view)
        },
        onFailure = {
          Result.failure(it)
        },
      )
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

    retrofitErrorHandler {
      api.listCommentVotes(authorization = account.bearer, form, force = force)
    }
      .fold(
        onSuccess = {
          Result.success(it)
        },
        onFailure = {
          Result.failure(it)
        },
      )
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

    retrofitErrorHandler {
      api.listPostVotes(authorization = account.bearer, form, force = force)
    }
      .fold(
        onSuccess = {
          Result.success(it)
        },
        onFailure = {
          Result.failure(it)
        },
      )
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

    retrofitErrorHandler { api.followCommunity(authorization = account.bearer, form) }
      .fold(
        onSuccess = {
          Result.success(it.community_view)
        },
        onFailure = {
          Result.failure(it)
        },
      )
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

    return retrofitErrorHandler {
      api.banUserFromCommunity(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.modUser(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.distinguishComment(authorization = account.bearer, args = form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.removeComment(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.createComment(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it.comment_view) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.editComment(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.comment_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun deleteComment(account: Account, commentId: CommentId): Result<CommentView> {
    val form = DeleteComment(
      auth = account.jwt,
      comment_id = commentId,
      deleted = true,
    )

    return retrofitErrorHandler {
      api.deleteComment(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.comment_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.createPost(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.post_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.editPost(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.post_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun deletePost(account: Account, id: PostId, delete: Boolean): Result<PostView> {
    val form = DeletePost(
      post_id = id,
      deleted = delete,
      auth = account.jwt,
    )

    return retrofitErrorHandler {
      api.deletePost(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.post_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.featurePost(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.post_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun lockPost(account: Account, id: PostId, locked: Boolean): Result<PostView> {
    val form = LockPost(
      post_id = id,
      locked = locked,
      auth = account.jwt,
    )

    return retrofitErrorHandler {
      api.lockPost(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it.post_view) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.removePost(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it.post_view) },
      onFailure = { Result.failure(it) },
    )
  }

  suspend fun uploadImage(
    account: Account,
    fileName: String,
    imageIs: InputStream,
  ): Result<UploadImageResult> {
    val url = "https://${api.instance}/pictrs/image"

    return retrofitErrorHandler {
      api.uploadImage(
        authorization = account.bearer,
        url = url,
        fileName = fileName,
        imageIs = imageIs,
      )
    }.fold(
      onSuccess = {
        val imageUrl = "$url/${it.files?.get(0)?.file}"
        Result.success(UploadImageResult(imageUrl))
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.blockCommunity(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.community_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.blockPerson(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.person_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.blockInstance(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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
  ): Result<GetPersonDetailsResponse> {
    val form = GetPersonDetails(
      person_id = personId,
      username = name,
      auth = account?.jwt,
      page = page,
      limit = limit,
      sort = sort,
      saved_only = savedOnly,
    )

    return retrofitErrorHandler {
      api.getPersonDetails(authorization = account?.bearer, form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.changePassword(authorization = account.bearer, changePassword)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.getReplies(authorization = account.bearer, form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it.replies)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.getPersonMentions(authorization = account.bearer, form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it.mentions)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.getPrivateMessages(authorization = account.bearer, form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it.private_messages)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.getPrivateMessageReports(authorization = account.bearer, form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.getPostReports(authorization = account.bearer, form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.createPrivateMessageReport(authorization = account.bearer, form)
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

    return retrofitErrorHandler {
      api.resolvePrivateMessageReport(authorization = account.bearer, form)
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

    return retrofitErrorHandler {
      api.resolvePostReport(authorization = account.bearer, form)
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

    return retrofitErrorHandler {
      api.getCommentReports(authorization = account.bearer, form, force = force)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.resolveCommentReport(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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
    return retrofitErrorHandler {
      api.markCommentReplyAsRead(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.comment_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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
    return retrofitErrorHandler {
      api.markPersonMentionAsRead(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.person_mention_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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
    return retrofitErrorHandler {
      api.markPrivateMessageAsRead(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.private_message_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun markAllAsRead(account: Account): Result<GetRepliesResponse> {
    val form = MarkAllAsRead(
      account.jwt,
    )
    return retrofitErrorHandler {
      api.markAllAsRead(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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
    return retrofitErrorHandler {
      api.createPrivateMessage(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.private_message_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun savePost(postId: PostId, save: Boolean, account: Account): Result<PostView> {
    val form = SavePost(postId, save, account.jwt)

    return retrofitErrorHandler {
      api.savePost(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.post_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun saveComment(
    commentId: CommentId,
    save: Boolean,
    account: Account,
  ): Result<CommentView> {
    val form = SaveComment(commentId, save, account.jwt)

    return retrofitErrorHandler {
      api.saveComment(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it.comment_view)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.createPostReport(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.createCommentReport(authorization = account.bearer, form)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun saveUserSettings(settings: SaveUserSettings): Result<Unit> {
    return retrofitErrorHandler {
      api.saveUserSettings(authorization = settings.auth.toBearer(), settings)
    }.fold(
      onSuccess = {
        Result.success(Unit)
      },
      onFailure = {
        Result.failure(it)
      },
    )
  }

  suspend fun resolveObject(q: String, account: Account): Result<ResolveObjectResponse> {
    val form = ResolveObject(
      q = q,
      auth = account.jwt,
    )

    return retrofitErrorHandler {
      api.resolveObject(authorization = account.bearer, form, force = false)
    }.fold(
      onSuccess = {
        Result.success(it)
      },
      onFailure = {
        Result.failure(it)
      },
    )
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

    return retrofitErrorHandler {
      api.banUserFromSite(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.removeCommunity(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.hideCommunity(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.purgePerson(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.purgeCommunity(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.purgePost(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.purgeComment(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.getModLogs(authorization = account?.bearer, form, force = force)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
  }

  suspend fun getUnreadRegistrationApplicationsCount(
    account: Account,
    force: Boolean,
  ): Result<GetUnreadRegistrationApplicationCountResponse> {
    val form = GetUnreadRegistrationApplicationCount(
      auth = account.jwt,
    )

    return retrofitErrorHandler {
      api.getRegistrationApplicationsCount(authorization = account.bearer, form, force = force)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.listRegistrationApplications(authorization = account.bearer, form, force = force)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.approveRegistrationApplication(authorization = account.bearer, form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
  }

  suspend fun listMedia(page: Long?, limit: Long?, account: Account, force: Boolean): Result<ListMediaResponse> {
    val form = ListMedia(
      page = page,
      limit = limit,
    )

    return retrofitErrorHandler {
      api.listMedia(authorization = account.bearer, form, force = force)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
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

    return retrofitErrorHandler {
      api.register(form)
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
  }

  suspend fun getCaptcha(): Result<GetCaptchaResponse> {
    return retrofitErrorHandler {
      api.getCaptcha()
    }.fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(it) },
    )
  }

  private suspend fun <T> retrofitErrorHandler(call: () -> Call<T>): Result<T> {
    val res = try {
      runInterruptible(Dispatchers.IO) {
        call().execute()
      }
    } catch (e: Exception) {
      if (e is SocketTimeoutException) {
        return Result.failure(com.idunnololz.summit.api.SocketTimeoutException())
      }
      if (e is UnknownHostException) {
        return Result.failure(NoInternetException())
      }
      if (e is CancellationException) {
        throw e
      }
      if (e is InterruptedIOException) {
        return Result.failure(e)
      }
      if (e is ConnectException) {
        return Result.failure(ConnectionException())
      }
      Log.e(TAG, "Exception fetching url", e)
      return Result.failure(e)
    }

    apiListenerManager.onRequestComplete(res)

    if (res.isSuccessful) {
      return Result.success(requireNotNull(res.body()))
    } else {
      val errorCode = res.code()
      val errorBody = res.errorBody()?.string()

      if (errorCode >= 500) {
        if (res.message().contains("only-if-cached", ignoreCase = true)) {
          // for some reason okhttp returns a 504 if we force cache with no internet
          return Result.failure(NoInternetException())
        }
        return Result.failure(
          ServerApiException(
            errorMessage = errorBody,
            errorCode = errorCode,
          ),
        )
      }

      if (errorCode == 429) {
        return Result.failure(RateLimitException(0L))
      }

      val errMsg = try {
        errorBody?.let {
          JSONObject(it).getString("error")
        } ?: run {
          res.code().toString()
        }
      } catch (e: Exception) {
        Log.e(TAG, "Exception parsing body", e)
        errorBody
      }

      if (errMsg?.equals("incorrect_login", ignoreCase = true) == true) {
        return Result.failure(ClientApiException(errMsg, errorCode))
      }

      if (errorCode == 401) {
        return Result.failure(NotAuthenticatedException())
      }

      if (errMsg?.contains("not_logged_in", ignoreCase = true) == true) {
        return Result.failure(NotAuthenticatedException())
      }
      if (errMsg == "rate_limit_error") { // might be safe to delete
        return Result.failure(RateLimitException(0L))
      }
      if (errMsg == "not_a_mod_or_admin") {
        return Result.failure(NotAModOrAdmin())
      }
      if (errMsg == "couldnt_find_object" || errMsg == "couldnt_find_community") {
        return Result.failure(CouldntFindObjectError())
      }
      // TODO: Remove these checks once v0.19 is out for everyone.
      if (errMsg?.contains("unknown variant") == true || (errorCode == 404 && res.raw().request.url.toString().contains("site/block"))) {
        return Result.failure(NewApiException("v0.19"))
      }

      if (errorCode == 403) {
        return Result.failure(ForbiddenException())
      }

      if (BuildConfig.DEBUG) {
        Log.e(
          "ApiError",
          "Code: $errorCode Error message: $errMsg Call: ${call().request().url}",
          RuntimeException(),
        )
      }

      if (errMsg?.contains("timeout", ignoreCase = true) == true) {
        return Result.failure(ServerTimeoutException(errorCode))
      }
      if (errMsg?.contains("the database system is not yet accepting connections", ignoreCase = true) == true) {
        // this is a 4xx error but it should be a 5xx error because it's server sided and retry-able
        return Result.failure(ServerApiException(null, 503))
      }

      return Result.failure(ClientApiException(errorMessage = errMsg, errorCode = errorCode))
    }
  }

  private fun getApiV3WithInstance(instance: String = Consts.DEFAULT_INSTANCE): LemmyLikeApi {
    return apis[instance]
      ?: newApiV3(instance).also {
        apis[instance] = it
      }
  }

  private fun newApiV3(instance: String = Consts.DEFAULT_INSTANCE): LemmyApiV3Adapter {
    return LemmyApiV3Adapter(
      Retrofit.Builder()
        .baseUrl("https://$instance/api/v3/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(LemmyApiV3::class.java),
      instance,
    )
  }
}

class UploadImageResult(
  val url: String,
)
