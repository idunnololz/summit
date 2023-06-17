package com.idunnololz.summit.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.idunnololz.summit.api.dto.BlockCommunity
import com.idunnololz.summit.api.dto.BlockCommunityResponse
import com.idunnololz.summit.api.dto.BlockPerson
import com.idunnololz.summit.api.dto.BlockPersonResponse
import com.idunnololz.summit.api.dto.CommentReportResponse
import com.idunnololz.summit.api.dto.CommentResponse
import com.idunnololz.summit.api.dto.CommunityResponse
import com.idunnololz.summit.api.dto.CreateComment
import com.idunnololz.summit.api.dto.CreateCommentLike
import com.idunnololz.summit.api.dto.CreateCommentReport
import com.idunnololz.summit.api.dto.CreatePost
import com.idunnololz.summit.api.dto.CreatePostLike
import com.idunnololz.summit.api.dto.CreatePostReport
import com.idunnololz.summit.api.dto.CreatePrivateMessage
import com.idunnololz.summit.api.dto.DeleteComment
import com.idunnololz.summit.api.dto.DeletePost
import com.idunnololz.summit.api.dto.EditComment
import com.idunnololz.summit.api.dto.EditPost
import com.idunnololz.summit.api.dto.FollowCommunity
import com.idunnololz.summit.api.dto.GetCommentsResponse
import com.idunnololz.summit.api.dto.GetCommunityResponse
import com.idunnololz.summit.api.dto.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.GetPersonMentionsResponse
import com.idunnololz.summit.api.dto.GetPostResponse
import com.idunnololz.summit.api.dto.GetPostsResponse
import com.idunnololz.summit.api.dto.GetRepliesResponse
import com.idunnololz.summit.api.dto.GetSiteMetadataResponse
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.GetUnreadCountResponse
import com.idunnololz.summit.api.dto.Login
import com.idunnololz.summit.api.dto.LoginResponse
import com.idunnololz.summit.api.dto.MarkAllAsRead
import com.idunnololz.summit.api.dto.MarkCommentReplyAsRead
import com.idunnololz.summit.api.dto.MarkPersonMentionAsRead
import com.idunnololz.summit.api.dto.MarkPrivateMessageAsRead
import com.idunnololz.summit.api.dto.PersonMentionResponse
import com.idunnololz.summit.api.dto.PictrsImages
import com.idunnololz.summit.api.dto.PostReportResponse
import com.idunnololz.summit.api.dto.PostResponse
import com.idunnololz.summit.api.dto.PrivateMessageResponse
import com.idunnololz.summit.api.dto.PrivateMessagesResponse
import com.idunnololz.summit.api.dto.SaveComment
import com.idunnololz.summit.api.dto.SavePost
import com.idunnololz.summit.api.dto.SaveUserSettings
import com.idunnololz.summit.api.dto.SearchResponse
import okhttp3.Cache
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.QueryMap
import retrofit2.http.Url

const val VERSION = "v3"
const val DEFAULT_INSTANCE = "lemmy.ml"

interface LemmyApi {
    @GET("site")
    suspend fun getSite(@QueryMap form: Map<String, String>): Response<GetSiteResponse>

    /**
     * Get / fetch posts, with various filters.
     */
    @GET("post/list")
    suspend fun getPosts(@QueryMap form: Map<String, String>): Response<GetPostsResponse>

    /**
     * Get / fetch a post.
     */
    @GET("post")
    suspend fun getPost(@QueryMap form: Map<String, String>): Response<GetPostResponse>

    /**
     * Log into lemmy.
     */
    @POST("user/login")
    suspend fun login(@Body form: Login): Response<LoginResponse>

    /**
     * Like / vote on a post.
     */
    @POST("post/like")
    suspend fun likePost(@Body form: CreatePostLike): Response<PostResponse>

    /**
     * Like / vote on a comment.
     */
    @POST("comment/like")
    suspend fun likeComment(@Body form: CreateCommentLike): Response<CommentResponse>

    /**
     * Create a comment.
     */
    @POST("comment")
    suspend fun createComment(@Body form: CreateComment): Response<CommentResponse>

    /**
     * Edit a comment.
     */
    @PUT("comment")
    suspend fun editComment(@Body form: EditComment): Response<CommentResponse>

    /**
     * Delete a comment.
     */
    @POST("comment/delete")
    suspend fun deleteComment(@Body form: DeleteComment): Response<CommentResponse>

    /**
     * Save a post.
     */
    @PUT("post/save")
    suspend fun savePost(@Body form: SavePost): Response<PostResponse>

    /**
     * Save a comment.
     */
    @PUT("comment/save")
    suspend fun saveComment(@Body form: SaveComment): Response<CommentResponse>

    /**
     * Get / fetch comments.
     */
    @GET("comment/list")
    suspend fun getComments(@QueryMap form: Map<String, String>): Response<GetCommentsResponse>

    /**
     * Get / fetch a community.
     */
    @GET("community")
    suspend fun getCommunity(@QueryMap form: Map<String, String>): Response<GetCommunityResponse>

    /**
     * Get the details for a person.
     */
    @GET("user")
    suspend fun getPersonDetails(@QueryMap form: Map<String, String>): Response<GetPersonDetailsResponse>

    /**
     * Get comment replies.
     */
    @GET("user/replies")
    suspend fun getReplies(@QueryMap form: Map<String, String>): Response<GetRepliesResponse>

    /**
     * Mark a comment as read.
     */
    @POST("comment/mark_as_read")
    suspend fun markCommentReplyAsRead(@Body form: MarkCommentReplyAsRead): Response<CommentResponse>

    /**
     * Mark a person mention as read.
     */
    @POST("user/mention/mark_as_read")
    suspend fun markPersonMentionAsRead(@Body form: MarkPersonMentionAsRead): Response<PersonMentionResponse>

    /**
     * Mark a private message as read.
     */
    @POST("private_message/mark_as_read")
    suspend fun markPrivateMessageAsRead(@Body form: MarkPrivateMessageAsRead): Response<PrivateMessageResponse>

    /**
     * Mark all replies as read.
     */
    @POST("user/mark_all_as_read")
    suspend fun markAllAsRead(@Body form: MarkAllAsRead): Response<GetRepliesResponse>

    /**
     * Get mentions for your user.
     */
    @GET("user/mention")
    suspend fun getPersonMentions(@QueryMap form: Map<String, String>): Response<GetPersonMentionsResponse>

    /**
     * Get / fetch private messages.
     */
    @GET("private_message/list")
    suspend fun getPrivateMessages(@QueryMap form: Map<String, String>): Response<PrivateMessagesResponse>

    /**
     * Create a private message.
     */
    @POST("private_message")
    suspend fun createPrivateMessage(@Body form: CreatePrivateMessage): Response<PrivateMessageResponse>

    /**
     * Get your unread counts
     */
    @GET("user/unread_count")
    suspend fun getUnreadCount(@QueryMap form: Map<String, String>): Response<GetUnreadCountResponse>

    /**
     * Follow / subscribe to a community.
     */
    @POST("community/follow")
    suspend fun followCommunity(@Body form: FollowCommunity): Response<CommunityResponse>

    /**
     * Create a post.
     */
    @POST("post")
    suspend fun createPost(@Body form: CreatePost): Response<PostResponse>

    /**
     * Edit a post.
     */
    @PUT("post")
    suspend fun editPost(@Body form: EditPost): Response<PostResponse>

    /**
     * Delete a post.
     */
    @POST("post/delete")
    suspend fun deletePost(@Body form: DeletePost): Response<PostResponse>

    /**
     * Search lemmy.
     */
    @GET("search")
    suspend fun search(@QueryMap form: Map<String, String>): Response<SearchResponse>

    /**
     * Fetch metadata for any given site.
     */
    @GET("post/site_metadata")
    suspend fun getSiteMetadata(@QueryMap form: Map<String, String>): Response<GetSiteMetadataResponse>

    /**
     * Report a comment.
     */
    @POST("comment/report")
    suspend fun createCommentReport(@Body form: CreateCommentReport): Response<CommentReportResponse>

    /**
     * Report a post.
     */
    @POST("post/report")
    suspend fun createPostReport(@Body form: CreatePostReport): Response<PostReportResponse>

    /**
     * Block a person.
     */
    @POST("user/block")
    suspend fun blockPerson(@Body form: BlockPerson): Response<BlockPersonResponse>

    /**
     * Block a community.
     */
    @POST("community/block")
    suspend fun blockCommunity(@Body form: BlockCommunity): Response<BlockCommunityResponse>

    /**
     * Save your user settings.
     */
    @PUT("user/save_user_settings")
    suspend fun saveUserSettings(@Body form: SaveUserSettings): Response<LoginResponse>

    /**
     * Upload an image.
     */
    @Multipart
    @POST
    suspend fun uploadImage(
        @Url url: String,
        @Header("Cookie") token: String,
        @Part filePart: MultipartBody.Part,
    ): Response<PictrsImages>

    companion object {

        private val apis = mutableMapOf<String, LemmyApiWithSite>()

        private var okHttpClient: OkHttpClient? = null

        private fun hasNetwork(context: Context): Boolean? {
            var isConnected: Boolean? = false // Initial Value
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
            if (activeNetwork != null && activeNetwork.isConnected)
                isConnected = true
            return isConnected
        }

        fun getInstance(context: Context, instance: String = DEFAULT_INSTANCE): LemmyApiWithSite {
            return apis[instance] ?: buildInstance(context, instance).also {
                apis[instance] = it
            }
        }

        private fun buildInstance(
            context: Context,
            instance: String = DEFAULT_INSTANCE
        ): LemmyApiWithSite {
            return buildApi(context, instance)
        }

        private fun getOkHttpClient(context: Context): OkHttpClient {
            okHttpClient?.let {
                return it
            }

            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY

            val cacheSize = (10 * 1024 * 1024).toLong() // 10MB
            val myCache = Cache(context.cacheDir, cacheSize)
            val okHttpClient = OkHttpClient.Builder()
                // Specify the cache we created earlier.
                .cache(myCache)
                // Add an Interceptor to the OkHttpClient.
                .addInterceptor { chain ->

                    // Get the request from the chain.
                    var request = chain.request()

                    /*
                    *  Leveraging the advantage of using Kotlin,
                    *  we initialize the request and change its header depending on whether
                    *  the device is connected to Internet or not.
                    */
                    request = if (hasNetwork(context)!!)
                    /*
                    *  If there is Internet, get the cache that was stored 5 seconds ago.
                    *  If the cache is older than 5 seconds, then discard it,
                    *  and indicate an error in fetching the response.
                    *  The 'max-age' attribute is responsible for this behavior.
                    */
                        request.newBuilder().header("Cache-Control", "public, max-age=" + 5).build()
                    else
                    /*
                    *  If there is no Internet, get the cache that was stored 7 days ago.
                    *  If the cache is older than 7 days, then discard it,
                    *  and indicate an error in fetching the response.
                    *  The 'max-stale' attribute is responsible for this behavior.
                    *  The 'only-if-cached' attribute indicates to not retrieve new data; fetch the cache only instead.
                    */
                        request.newBuilder().header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7).build()
                    // End of if-else statement

                    // Add the modified request to the chain.
                    chain.proceed(request)
                }
                .addInterceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()
                        .header("User-Agent", "Jerboa")
                    val newRequest = requestBuilder.build()
                    chain.proceed(newRequest)
                }
                .addInterceptor(interceptor)
                .build()
                .also {
                    okHttpClient = it
                }

            return okHttpClient
        }

        private fun buildApi(context: Context, instance: String): LemmyApiWithSite {
            return LemmyApiWithSite(
                Retrofit.Builder()
                    .baseUrl("https://$instance/api/$VERSION/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(getOkHttpClient(context))
                    .build()
                    .create(LemmyApi::class.java),
                "https://$instance"
            )
        }
    }
}

class LemmyApiWithSite(
    private val lemmyApi: LemmyApi,
    val site: String,
): LemmyApi by lemmyApi