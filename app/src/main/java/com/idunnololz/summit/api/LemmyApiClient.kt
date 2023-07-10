package com.idunnololz.summit.api

import android.content.Context
import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.dto.BlockCommunity
import com.idunnololz.summit.api.dto.BlockPerson
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentReplyId
import com.idunnololz.summit.api.dto.CommentReplyView
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.CreateComment
import com.idunnololz.summit.api.dto.CreateCommentLike
import com.idunnololz.summit.api.dto.CreatePost
import com.idunnololz.summit.api.dto.CreatePostLike
import com.idunnololz.summit.api.dto.CreatePrivateMessage
import com.idunnololz.summit.api.dto.DeleteComment
import com.idunnololz.summit.api.dto.DeletePost
import com.idunnololz.summit.api.dto.EditComment
import com.idunnololz.summit.api.dto.EditPost
import com.idunnololz.summit.api.dto.FollowCommunity
import com.idunnololz.summit.api.dto.GetComments
import com.idunnololz.summit.api.dto.GetCommunity
import com.idunnololz.summit.api.dto.GetPersonDetails
import com.idunnololz.summit.api.dto.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.GetPersonMentions
import com.idunnololz.summit.api.dto.GetPost
import com.idunnololz.summit.api.dto.GetPosts
import com.idunnololz.summit.api.dto.GetPrivateMessages
import com.idunnololz.summit.api.dto.GetReplies
import com.idunnololz.summit.api.dto.GetSite
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.GetUnreadCount
import com.idunnololz.summit.api.dto.GetUnreadCountResponse
import com.idunnololz.summit.api.dto.ListCommunities
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.Login
import com.idunnololz.summit.api.dto.MarkCommentReplyAsRead
import com.idunnololz.summit.api.dto.MarkPersonMentionAsRead
import com.idunnololz.summit.api.dto.MarkPostAsRead
import com.idunnololz.summit.api.dto.MarkPrivateMessageAsRead
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PersonMentionId
import com.idunnololz.summit.api.dto.PersonMentionView
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.PrivateMessageId
import com.idunnololz.summit.api.dto.PrivateMessageView
import com.idunnololz.summit.api.dto.SaveUserSettings
import com.idunnololz.summit.api.dto.Search
import com.idunnololz.summit.api.dto.SearchResponse
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.util.Utils.serializeToMap
import com.idunnololz.summit.util.retry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

const val COMMENTS_DEPTH_MAX = 6

class LemmyApiClient @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "LemmyApiClient"

        const val API_VERSION = "v3"
        val DEFAULT_INSTANCE = CommonLemmyInstance.LemmyMl.instance

        val DEFAULT_LEMMY_INSTANCES = listOf(
            CommonLemmyInstance.Beehaw.instance,
            "feddit.de",
            "feddit.it",
            "lemmy.ca",
            CommonLemmyInstance.LemmyMl.instance,
            "lemmy.one",
            CommonLemmyInstance.LemmyWorld.instance,
            "lemmygrad.ml",
            "midwest.social",
            "mujico.org",
            "sh.itjust.works",
            "slrpnk.net",
            "sopuli.xyz",
            "szmer.info",
        )
    }

    private var api = LemmyApi.getInstance(context)
    val okHttpClient = LemmyApi.okHttpClient(context)

    fun changeInstance(newInstance: String) {
        api = LemmyApi.getInstance(context, newInstance)
    }

    fun defaultInstance() {
        api = LemmyApi.getInstance(context)
    }

    fun clearCache() {
        okHttpClient.cache?.evictAll()
    }

    suspend fun fetchPosts(
        account: Account?,
        communityIdOrName: Either<Int, String>? = null,
        sortType: SortType,
        listingType: ListingType,
        page: Int,
        limit: Int? = null,
        force: Boolean,
    ): Result<List<PostView>> {
        val communityId = communityIdOrName?.fold({ it }, { null })
        val communityName = communityIdOrName?.fold({ null }, { it })

        val form = try {
            GetPosts(
                community_id = communityId,
                community_name = communityName,
                sort = sortType,
                type_ = listingType,
                page = page,
                limit = limit,
                auth = account?.jwt,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching posts", e)
            false
        }

        return retrofitErrorHandler {
            if (force) {
                api.getPostsNoCache(form = form.serializeToMap())
            } else {
                api.getPosts(form = form.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it.posts)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun fetchPost(
        account: Account?,
        id: Either<PostId, CommentId>,
        force: Boolean,
    ): Result<PostView> {
        val postForm = id.fold({
            GetPost(id = it, auth = account?.jwt)
        }, {
            GetPost(comment_id = it, auth = account?.jwt)
        })

        return retrofitErrorHandler {
            if (force) {
                api.getPostNoCache(form = postForm.serializeToMap())
            } else {
                api.getPost(form = postForm.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it.post_view)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun markPostAsRead(
        postId: PostId,
        read: Boolean,
        account: Account,
    ): Result<PostView> {
        val form = MarkPostAsRead(
            post_id = postId,
            read = read,
            auth = account.jwt
        )

        return retrofitErrorHandler {
            api.markPostAsRead(form = form)
        }.fold(
            onSuccess = {
                Result.success(it.post_view)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun fetchComments(
        account: Account?,
        id: Either<PostId, CommentId>,
        sort: CommentSortType,
        force: Boolean,
    ): Result<List<CommentView>> {

        val commentsForm = id.fold({
            GetComments(
                max_depth = COMMENTS_DEPTH_MAX,
                type_ = ListingType.All,
                post_id = it,
                sort = sort,
                auth = account?.jwt,
            )
        }, {
            GetComments(
                max_depth = COMMENTS_DEPTH_MAX,
                type_ = ListingType.All,
                parent_id = it,
                sort = sort,
                auth = account?.jwt,
            )
        })

        return retrofitErrorHandler {
            if (force) {
                api.getCommentsNoCache(
                    commentsForm
                        .serializeToMap(),
                )
            } else {
                api.getComments(
                    commentsForm
                        .serializeToMap(),
                )
            }
        }.fold(
            onSuccess = {
                Result.success(it.comments)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun getCommunity(
        account: Account?,
        idOrName: Either<Int, String>,
        force: Boolean,
    ): Result<CommunityView> {
        val form = idOrName.fold({ id ->
            GetCommunity(id = id, auth = account?.jwt)
        }, { name ->
            GetCommunity(name = name, auth = account?.jwt)
        })

        return retrofitErrorHandler {
            if (force) {
                api.getCommunityNoCache(form = form.serializeToMap())
            } else {
                api.getCommunity(form = form.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it.community_view)
            },
            onFailure = {
                Result.failure(it)
            }
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
        creatorId: Int? = null,
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

        return retrofitErrorHandler { api.search(form = form.serializeToMap()) }.fold(
            onSuccess = {
                Result.success(it)
            },
            onFailure = {
                Result.failure(it)
            }
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
            api.getCommunityList(form = form.serializeToMap())
        }.fold(
            onSuccess = {
                Result.success(it.communities)
            },
            onFailure = {
                Result.failure(it)
            }
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

        return retrofitErrorHandler { api.login(form = form) }
            .fold(
                onSuccess = {
                    Result.success(it.jwt)
                },
                onFailure = {
                    changeInstance(originalInstance)
                    Result.failure(it)
                }
            )
    }

    suspend fun fetchSiteWithRetry(
        auth: String?,
        force: Boolean,
    ): Result<GetSiteResponse> = retry {
        val form = GetSite(auth = auth)

        retrofitErrorHandler {
            if (force) {
                api.getSiteNoCache(form = form.serializeToMap())
            } else {
                api.getSite(form = form.serializeToMap())
            }
        }
            .fold(
                onSuccess = {
                    Result.success(it)
                },
                onFailure = {
                    Result.failure(it)
                }
            )
    }

    suspend fun fetchUnreadCount(
        force: Boolean,
        account: Account
    ): Result<GetUnreadCountResponse> {
        val form = GetUnreadCount(account.jwt)

        return retrofitErrorHandler {
            if (force) {
                api.getUnreadCountNoCache(form.serializeToMap())
            } else {
                api.getUnreadCount(form.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun likePostWithRetry(
        postId: Int,
        score: Int,
        account: Account,
    ): Result<PostView> = retry {

        val form = CreatePostLike(
            post_id = postId,
            score = score,
            auth = account.jwt,
        )

        retrofitErrorHandler { api.likePost(form) }
            .fold(
                onSuccess = {
                    Result.success(it.post_view)
                },
                onFailure = {
                    Result.failure(it)
                }
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

        retrofitErrorHandler { api.likeComment(form) }
            .fold(
                onSuccess = {
                    Result.success(it.comment_view)
                },
                onFailure = {
                    Result.failure(it)
                }
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

        retrofitErrorHandler { api.followCommunity(form) }
            .fold(
                onSuccess = {
                    Result.success(it.community_view)
                },
                onFailure = {
                    Result.failure(it)
                }
            )
    }

    suspend fun createComment(
        account: Account,
        content: String,
        postId: PostId,
        parentId: CommentId?
    ): Result<CommentView> {
        val form = CreateComment(
            auth = account.jwt,
            content = content,
            post_id = postId,
            parent_id = parentId,
        )

        return retrofitErrorHandler {
            api.createComment(form)
        }.fold(
            onSuccess = {
                Result.success(it.comment_view)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun editComment(
        account: Account,
        content: String,
        commentId: CommentId
    ): Result<CommentView> {
        val form = EditComment(
            auth = account.jwt,
            content = content,
            comment_id = commentId,
        )

        return retrofitErrorHandler {
            api.editComment(form)
        }.fold(
            onSuccess = {
                Result.success(it.comment_view)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun deleteComment(
        account: Account,
        commentId: CommentId
    ): Result<CommentView> {
        val form = DeleteComment(
            auth = account.jwt,
            comment_id = commentId,
            deleted = true,
        )

        return retrofitErrorHandler {
            api.deleteComment(form)
        }.fold(
            onSuccess = {
                Result.success(it.comment_view)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun createPost(
        name: String,
        body: String?,
        url: String?,
        isNsfw: Boolean,
        account: Account,
        communityId: CommunityId,
    ): Result<PostView> {
        val form = CreatePost(
            name = name,
            community_id = communityId,
            url = url,
            body = body,
            nsfw = isNsfw,
            auth = account.jwt
        )

        return retrofitErrorHandler {
            api.createPost(form)
        }.fold(
            onSuccess = {
                Result.success(it.post_view)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun editPost(
        postId: PostId,
        name: String,
        body: String?,
        url: String?,
        isNsfw: Boolean,
        account: Account,
    ): Result<PostView> {
        val form = EditPost(
            post_id = postId,
            name = name,
            url = url,
            body = body,
            nsfw = isNsfw,
            auth = account.jwt
        )

        return retrofitErrorHandler {
            api.editPost(form)
        }.fold(
            onSuccess = {
                Result.success(it.post_view)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun deletePost(account: Account, id: PostId): Result<PostView> {
        val form = DeletePost(
            post_id = id,
            deleted = true,
            auth = account.jwt,
        )

        return retrofitErrorHandler {
            api.deletePost(form)
        }.fold(
            onSuccess = {
                Result.success(it.post_view)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun uploadImage(
        account: Account,
        fileName: String,
        imageIs: InputStream
    ): Result<UploadImageResult> {
        val part = MultipartBody.Part.createFormData(
            "images[]",
            fileName,
            imageIs.readBytes().toRequestBody(),
        )
        val url = "https://${api.instance}/pictrs/image"
        val cookie = "jwt=${account.jwt}"

        return retrofitErrorHandler {
            api.uploadImage(url, cookie, part)
        }.fold(
            onSuccess = {
                val imageUrl = "$url/${it.files?.get(0)?.file}"
                Result.success(UploadImageResult(imageUrl))
            },
            onFailure = {
                Result.failure(it)
            }
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
            api.blockCommunity(form)
        }.fold(
            onSuccess = {
                Result.success(it.community_view)
            },
            onFailure = {
                Result.failure(it)
            }
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
            api.blockPerson(form)
        }.fold(
            onSuccess = {
                Result.success(it.person_view)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun fetchPerson(
        personId: PersonId?,
        name: String?,
        account: Account?
    ): Result<GetPersonDetailsResponse> {
        val form = GetPersonDetails(
            person_id = personId,
            username = name,
            auth = account?.jwt,
        )

        return retrofitErrorHandler {
            api.getPersonDetails(form.serializeToMap())
        }.fold(
            onSuccess = {
                Result.success(it)
            },
            onFailure = {
                Result.failure(it)
            }
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
            sort = sort, page = page, limit = limit, unread_only = unreadOnly, auth = account.jwt
        )

        return retrofitErrorHandler {
            if (force) {
                api.getRepliesNoCache(form.serializeToMap())
            } else {
                api.getReplies(form.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it.replies)
            },
            onFailure = {
                Result.failure(it)
            }
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
            sort, page, limit, unreadOnly, account.jwt
        )

        return retrofitErrorHandler {
            if (force) {
                api.getPersonMentionsNoCache(form.serializeToMap())
            } else {
                api.getPersonMentions(form.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it.mentions)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun fetchPrivateMessages(
        unreadOnly: Boolean? = null,
        page: Int? = null,
        limit: Int? = null,
        account: Account,
        force: Boolean,
    ): Result<List<PrivateMessageView>> {
        val form = GetPrivateMessages(
            unreadOnly, page, limit, account.jwt
        )

        return retrofitErrorHandler {
            if (force) {
                api.getPrivateMessagesNoCache(form.serializeToMap())
            } else {
                api.getPrivateMessages(form.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it.private_messages)
            },
            onFailure = {
                Result.failure(it)
            }
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
            account.jwt
        )
        return retrofitErrorHandler {
            api.markCommentReplyAsRead(form)
        }.fold(
            onSuccess = {
                Result.success(it.comment_view)
            },
            onFailure = {
                Result.failure(it)
            }
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
            account.jwt
        )
        return retrofitErrorHandler {
            api.markPersonMentionAsRead(form)
        }.fold(
            onSuccess = {
                Result.success(it.person_mention_view)
            },
            onFailure = {
                Result.failure(it)
            }
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
            account.jwt
        )
        return retrofitErrorHandler {
            api.markPrivateMessageAsRead(form)
        }.fold(
            onSuccess = {
                Result.success(it.private_message_view)
            },
            onFailure = {
                Result.failure(it)
            }
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
            api.createPrivateMessage(form)
        }.fold(
            onSuccess = {
                Result.success(it.private_message_view)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    suspend fun saveUserSettings(settings: SaveUserSettings): Result<Unit> {
        return retrofitErrorHandler {
            api.saveUserSettings(settings)
        }.fold(
            onSuccess = {
                Result.success(Unit)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    val instance: String
        get() = api.instance

    private suspend fun <T> retrofitErrorHandler(
        call: suspend () -> Call<T>
    ): Result<T> {
        val res = try {
            withContext(Dispatchers.IO) {
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
            Log.e(TAG, "Exception fetching url", e)
            return Result.failure(e)
        }
        if (res.isSuccessful) {
            return Result.success(requireNotNull(res.body()))
        } else {
            val errorCode = res.code()

            if (errorCode >= 500) {
                if (res.message().contains("only-if-cached", ignoreCase = true)) {
                    // for some reason okhttp returns a 504 if we force cache with no internet
                    return Result.failure(NoInternetException())
                }
                return Result.failure(ServerApiException(errorCode))
            }

            if (errorCode == 401) {
                return Result.failure(NotAuthenticatedException())
            }

            val errorBody = res.errorBody()?.string()
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

            if (errMsg?.contains("not_logged_in", ignoreCase = true) == true) {
                return Result.failure(NotAuthenticatedException())
            }

            Log.e("ApiError", "Code: ${errorCode} Error message: ${errMsg}", RuntimeException())

            if (errMsg?.contains("timeout", ignoreCase = true) == true) {
                return Result.failure(ServerTimeoutException(errorCode))
            }

            return Result.failure(ClientApiException(errMsg, errorCode))
        }
    }
}

class UploadImageResult(
    val url: String
)