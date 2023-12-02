package com.idunnololz.summit.api

import android.content.Context
import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.dto.*
import com.idunnololz.summit.util.Utils.serializeToMap
import com.idunnololz.summit.util.retry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

const val COMMENTS_DEPTH_MAX = 6

class LemmyApiClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "LemmyApiClient"

        const val API_VERSION = "v3"
        val DEFAULT_INSTANCE = CommonLemmyInstance.LemmyMl.instance

        val DEFAULT_LEMMY_INSTANCES = listOf(
            CommonLemmyInstance.Beehaw.instance,
            "feddit.de",
            "feddit.it",
            "lemm.ee",
            "lemmy.blahaj.zone",
            "lemmy.ca",
            "lemmy.dbzer0.com",
            CommonLemmyInstance.LemmyMl.instance,
            "lemmy.one",
            CommonLemmyInstance.LemmyWorld.instance,
            "lemmygrad.ml",
            "lemmynsfw.com",
            "midwest.social",
            "mujico.org",
            "programming.dev",
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
                api.getPostsNoCache(authorization = account?.bearer, form = form.serializeToMap())
            } else {
                api.getPosts(authorization = account?.bearer, form = form.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it.posts)
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
    ): Result<PostView> {
        val postForm = id.fold({
            GetPost(id = it, auth = account?.jwt)
        }, {
            GetPost(comment_id = it, auth = account?.jwt)
        },)

        return retrofitErrorHandler {
            if (force) {
                api.getPostNoCache(authorization = account?.bearer, form = postForm.serializeToMap())
            } else {
                api.getPost(authorization = account?.bearer, form = postForm.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it.post_view)
            },
            onFailure = {
                Result.failure(it)
            },
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
            auth = account.jwt,
        )

        return retrofitErrorHandler {
            api.markPostAsRead(authorization = account.bearer, form = form)
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
        val form = try {
            GetPosts(
                community_id = null,
                community_name = null,
                sort = SortType.New,
                type_ = ListingType.All,
                page = page,
                limit = limit,
                saved_only = true,
                auth = account?.jwt,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching posts", e)
            false
        }

        return retrofitErrorHandler {
            if (force) {
                api.getPostsNoCache(authorization = account?.bearer, form = form.serializeToMap())
            } else {
                api.getPosts(authorization = account?.bearer, form = form.serializeToMap())
            }
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
            if (force) {
                api.getCommentsNoCache(
                    authorization = account?.bearer,
                    commentsForm
                        .serializeToMap(),
                )
            } else {
                api.getComments(
                    authorization = account?.bearer,
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
            },
        )
    }

    suspend fun fetchComments(
        account: Account?,
        id: Either<PostId, CommentId>,
        sort: CommentSortType,
        maxDepth: Int?,
        force: Boolean = false,
    ): Result<List<CommentView>> {
        val commentsForm = id.fold({
            GetComments(
                max_depth = maxDepth ?: COMMENTS_DEPTH_MAX,
                type_ = ListingType.All,
                post_id = it,
                sort = sort,
                auth = account?.jwt,
            )
        }, {
            GetComments(
                max_depth = maxDepth ?: COMMENTS_DEPTH_MAX,
                type_ = ListingType.All,
                parent_id = it,
                sort = sort,
                auth = account?.jwt,
            )
        },)

        return retrofitErrorHandler {
            if (force) {
                api.getCommentsNoCache(
                    authorization = account?.bearer,
                    commentsForm
                        .serializeToMap(),
                )
            } else {
                api.getComments(
                    authorization = account?.bearer,
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

        val form = GetCommunity(name = finalName, auth = account?.jwt)

        return retrofitErrorHandler {
            if (force) {
                api.getCommunityNoCache(authorization = account?.bearer, form = form.serializeToMap())
            } else {
                api.getCommunity(authorization = account?.bearer, form = form.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it.community_view)
            },
            onFailure = {
                Result.failure(it)
            },
        )
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
        },)

        return retrofitErrorHandler {
            if (force) {
                api.getCommunityNoCache(authorization = account?.bearer, form = form.serializeToMap())
            } else {
                api.getCommunity(authorization = account?.bearer, form = form.serializeToMap())
            }
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
            if (force) {
                api.searchNoCache(authorization = account?.bearer, form = form.serializeToMap())
            } else {
                api.search(authorization = account?.bearer, form = form.serializeToMap())
            }
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
            api.getCommunityList(authorization = account?.bearer, form = form.serializeToMap())
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

        return retrofitErrorHandler { api.login(form = form) }
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

    suspend fun fetchSiteWithRetry(
        auth: String?,
        force: Boolean,
    ): Result<GetSiteResponse> = retry {
        val form = GetSite(auth = auth)

        retrofitErrorHandler {
            if (force) {
                api.getSiteNoCache(authorization = auth?.toBearer(), form = form.serializeToMap())
            } else {
                api.getSite(authorization = auth?.toBearer(), form = form.serializeToMap())
            }
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

    suspend fun fetchUnreadCount(
        force: Boolean,
        account: Account,
    ): Result<GetUnreadCountResponse> {
        val form = GetUnreadCount(account.jwt)

        return retrofitErrorHandler {
            if (force) {
                api.getUnreadCountNoCache(authorization = account?.bearer, form.serializeToMap())
            } else {
                api.getUnreadCount(authorization = account?.bearer, form.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it)
            },
            onFailure = {
                Result.failure(it)
            },
        )
    }

    suspend fun fetchUnresolvedReportCount(
        force: Boolean,
        account: Account,
    ): Result<GetReportCountResponse> {
        val form = GetReportCount(
            null,
            account.jwt,
        )

        return retrofitErrorHandler {
            if (force) {
                api.getReportCountNoCache(authorization = account?.bearer, form.serializeToMap())
            } else {
                api.getReportCount(authorization = account?.bearer, form.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it)
            },
            onFailure = {
                Result.failure(it)
            },
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

        retrofitErrorHandler { api.likePost(authorization = account?.bearer, form) }
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

        retrofitErrorHandler { api.likeComment(authorization = account?.bearer, form) }
            .fold(
                onSuccess = {
                    Result.success(it.comment_view)
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

        retrofitErrorHandler { api.followCommunity(authorization = account?.bearer, form) }
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
        expiresDays: Int?,
        account: Account,
    ): Result<BanFromCommunityResponse> {
        val form = BanFromCommunity(
            communityId,
            personId,
            ban,
            removeData,
            reason,
            expiresDays,
            account.jwt,
        )

        return retrofitErrorHandler {
            api.banUserFromCommunity(authorization = account?.bearer, form)
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
            api.modUser(authorization = account?.bearer, form)
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
            api.distinguishComment(authorization = account?.bearer, form)
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
    ): Result<CommentView> {
        val form = CreateComment(
            auth = account.jwt,
            content = content,
            post_id = postId,
            parent_id = parentId,
        )

        return retrofitErrorHandler {
            api.createComment(authorization = account?.bearer, form)
        }.fold(
            onSuccess = { Result.success(it.comment_view) },
            onFailure = { Result.failure(it) },
        )
    }

    suspend fun editComment(
        account: Account,
        content: String,
        commentId: CommentId,
    ): Result<CommentView> {
        val form = EditComment(
            auth = account.jwt,
            content = content,
            comment_id = commentId,
        )

        return retrofitErrorHandler {
            api.editComment(authorization = account?.bearer, form)
        }.fold(
            onSuccess = {
                Result.success(it.comment_view)
            },
            onFailure = {
                Result.failure(it)
            },
        )
    }

    suspend fun deleteComment(
        account: Account,
        commentId: CommentId,
    ): Result<CommentView> {
        val form = DeleteComment(
            auth = account.jwt,
            comment_id = commentId,
            deleted = true,
        )

        return retrofitErrorHandler {
            api.deleteComment(authorization = account?.bearer, form)
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
        )

        return retrofitErrorHandler {
            api.createPost(authorization = account?.bearer, form)
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
        account: Account,
    ): Result<PostView> {
        val form = EditPost(
            post_id = postId,
            name = name,
            url = url,
            body = body,
            nsfw = isNsfw,
            auth = account.jwt,
        )

        return retrofitErrorHandler {
            api.editPost(authorization = account?.bearer, form)
        }.fold(
            onSuccess = {
                Result.success(it.post_view)
            },
            onFailure = {
                Result.failure(it)
            },
        )
    }

    suspend fun deletePost(account: Account, id: PostId): Result<PostView> {
        val form = DeletePost(
            post_id = id,
            deleted = true,
            auth = account.jwt,
        )

        return retrofitErrorHandler {
            api.deletePost(authorization = account?.bearer, form)
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
            api.featurePost(authorization = account?.bearer, form)
        }.fold(
            onSuccess = {
                Result.success(it.post_view)
            },
            onFailure = {
                Result.failure(it)
            },
        )
    }

    suspend fun lockPost(
        account: Account,
        id: PostId,
        locked: Boolean,
    ): Result<PostView> {
        val form = LockPost(
            post_id = id,
            locked = locked,
            auth = account.jwt,
        )

        return retrofitErrorHandler {
            api.lockPost(authorization = account?.bearer, form)
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
            api.removePost(authorization = account?.bearer, form)
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
        val part = MultipartBody.Part.createFormData(
            "images[]",
            fileName,
            imageIs.readBytes().toRequestBody(),
        )
        val url = "https://${api.instance}/pictrs/image"
        val cookie = "jwt=${account.jwt}"

        return retrofitErrorHandler {
            api.uploadImage(authorization = account?.bearer, url, cookie, part)
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
            api.blockCommunity(authorization = account?.bearer, form)
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
            api.blockPerson(authorization = account?.bearer, form)
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
            api.blockInstance(authorization = account?.bearer, form)
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
            if (force) {
                api.getPersonDetailsNoCache(authorization = account?.bearer, form.serializeToMap())
            } else {
                api.getPersonDetails(authorization = account?.bearer, form.serializeToMap())
            }
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
            if (force) {
                api.getRepliesNoCache(authorization = account?.bearer, form.serializeToMap())
            } else {
                api.getReplies(authorization = account?.bearer, form.serializeToMap())
            }
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
            if (force) {
                api.getPersonMentionsNoCache(authorization = account?.bearer, form.serializeToMap())
            } else {
                api.getPersonMentions(authorization = account?.bearer, form.serializeToMap())
            }
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
        account: Account,
        force: Boolean,
    ): Result<List<PrivateMessageView>> {
        val form = GetPrivateMessages(
            unreadOnly,
            page,
            limit,
            account.jwt,
        )

        return retrofitErrorHandler {
            if (force) {
                api.getPrivateMessagesNoCache(authorization = account?.bearer, form.serializeToMap())
            } else {
                api.getPrivateMessages(authorization = account?.bearer, form.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it.private_messages)
            },
            onFailure = {
                Result.failure(it)
            },
        )
    }

    suspend fun fetchReportMessages(
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
            if (force) {
                api.getReportMessagesNoCache(authorization = account?.bearer, form.serializeToMap())
            } else {
                api.getReportMessages(authorization = account?.bearer, form.serializeToMap())
            }
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
            if (force) {
                api.getPostReportsNoCache(authorization = account?.bearer, form.serializeToMap())
            } else {
                api.getPostReports(authorization = account?.bearer, form.serializeToMap())
            }
        }.fold(
            onSuccess = {
                Result.success(it)
            },
            onFailure = {
                Result.failure(it)
            },
        )
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
            api.resolvePostReport(authorization = account?.bearer, form)
        }.fold(
            onSuccess = {
                Result.success(it)
            },
            onFailure = {
                Result.failure(it)
            },
        )
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
            if (force) {
                api.getCommentReportsNoCache(authorization = account?.bearer, form.serializeToMap())
            } else {
                api.getCommentReports(authorization = account?.bearer, form.serializeToMap())
            }
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
            api.resolveCommentReport(authorization = account?.bearer, form)
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
            api.markCommentReplyAsRead(authorization = account?.bearer, form)
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
            api.markPersonMentionAsRead(authorization = account?.bearer, form)
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
            api.markPrivateMessageAsRead(authorization = account?.bearer, form)
        }.fold(
            onSuccess = {
                Result.success(it.private_message_view)
            },
            onFailure = {
                Result.failure(it)
            },
        )
    }

    suspend fun markAllAsRead(
        account: Account,
    ): Result<GetRepliesResponse> {
        val form = MarkAllAsRead(
            account.jwt,
        )
        return retrofitErrorHandler {
            api.markAllAsRead(authorization = account?.bearer, form)
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
            api.createPrivateMessage(authorization = account?.bearer, form)
        }.fold(
            onSuccess = {
                Result.success(it.private_message_view)
            },
            onFailure = {
                Result.failure(it)
            },
        )
    }

    suspend fun savePost(
        postId: PostId,
        save: Boolean,
        account: Account,
    ): Result<PostView> {
        val form = SavePost(postId, save, account.jwt)

        return retrofitErrorHandler {
            api.savePost(authorization = account?.bearer, form)
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
            api.saveComment(authorization = account?.bearer, form)
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
            api.createPostReport(authorization = account?.bearer, form)
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
            api.createCommentReport(authorization = account?.bearer, form)
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

    suspend fun resolveObject(
        q: String,
        account: Account,
    ): Result<ResolveObjectResponse> {
        val form = ResolveObject(
            q = q,
            auth = account.jwt,
        )

        return retrofitErrorHandler {
            api.resolveObject(authorization = account?.bearer, form.serializeToMap())
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
        expiresDays: Int?,
        account: Account,
    ): Result<BanPersonResponse> {
        val form = BanPerson(
            person_id = personId,
            ban = ban,
            remove_data = removeData,
            reason = reason,
            expires = expiresDays,
            auth = account.jwt,
        )

        return retrofitErrorHandler {
            api.banUserFromSite(authorization = account.bearer, form)
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
     * @param personId the id of the mod
     * @param otherPersonId the id of the person that the action was against
     */
    suspend fun fetchModLogs(
        personId: PersonId? = null,
        communityId: CommunityId? = null,
        page: Int? = null,
        limit: Int? = null,
        actionType: ModlogActionType? /* "All" | "ModRemovePost" | "ModLockPost" | "ModFeaturePost" | "ModRemoveComment" | "ModRemoveCommunity" | "ModBanFromCommunity" | "ModAddCommunity" | "ModTransferCommunity" | "ModAdd" | "ModBan" | "ModHideCommunity" | "AdminPurgePerson" | "AdminPurgeCommunity" | "AdminPurgePost" | "AdminPurgeComment" */ = null,
        otherPersonId: PersonId? = null,
        account: Account? = null,
    ): Result<GetModlogResponse> {
        val form = GetModlog(
            personId,
            communityId,
            page,
            limit,
            actionType,
            otherPersonId,
            account?.jwt
        )

        return retrofitErrorHandler {
            api.getModLogs(authorization = account?.bearer, form.serializeToMap())
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it) },
        )
    }

    val instance: String
        get() = api.instance

    private suspend fun <T> retrofitErrorHandler(
        call: () -> Call<T>,
    ): Result<T> {
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
            if (errMsg == "rate_limit_error") {
                return Result.failure(RateLimitException(0L))
            }
            // TODO: Remove these checks once v0.19 is out for everyone.
            if (errMsg?.contains("unknown variant") == true || (errorCode == 404 && res.raw().request.url.toString().contains("site/block"))) {
                return Result.failure(NewApiException("v0.19"))
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
                return Result.failure(ServerApiException(503))
            }

            return Result.failure(ClientApiException(errMsg, errorCode))
        }
    }

    private val Account.bearer: String
        get() = "Bearer $jwt"

    private fun String.toBearer(): String =
        "Bearer $this"
}

class UploadImageResult(
    val url: String,
)
