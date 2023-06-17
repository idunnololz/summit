package com.idunnololz.summit.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.GetComments
import com.idunnololz.summit.api.dto.GetCommunity
import com.idunnololz.summit.api.dto.GetPost
import com.idunnololz.summit.api.dto.GetPosts
import com.idunnololz.summit.api.dto.GetPostsResponse
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.util.Utils.serializeToMap
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

typealias PostId = Int
typealias CommentId = Int

const val COMMENTS_DEPTH_MAX = 6

@ViewModelScoped
class LemmyApiClient @Inject constructor(@ApplicationContext private val context: Context){

    companion object {
        private const val TAG = "LemmyApiClient"
    }

    private var api = LemmyApi.getInstance(context)

    fun changeInstance(newInstance: String) {
        api = LemmyApi.getInstance(context, newInstance)
    }

    suspend fun fetchPosts(
        account: Account?,
        communityIdOrName: Either<Int, String>? = null,
        sortType: SortType,
        listingType: ListingType,
        page: Int,
    ): List<PostView> {
        var posts = listOf<PostView>()

        val communityId = communityIdOrName?.fold({ it }, { null })
        val communityName = communityIdOrName?.fold({ null }, { it })

        try {
            val form = GetPosts(
                community_id = communityId,
                community_name = communityName,
                sort = sortType.toString(),
                type_ = listingType.toString(),
                page = page,
                auth = account?.jwt,
            )
            posts = retrofitErrorHandler<GetPostsResponse>(api.getPosts(form = form.serializeToMap())).posts
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching posts", e)
        }

        return posts
    }

    suspend fun fetchPost(
        account: Account?,
        id: Either<PostId, CommentId>,
    ): PostView {
        val postForm = id.fold({
            GetPost(id = it, auth = account?.jwt)
        }, {
            GetPost(comment_id = it, auth = account?.jwt)
        })

        val postOut = retrofitErrorHandler(api.getPost(form = postForm.serializeToMap()))

        return postOut.post_view
    }

    suspend fun fetchComments(
        account: Account?,
        id: Either<PostId, CommentId>,
    ): List<CommentView> {

        val commentsForm = id.fold({
            GetComments(
                max_depth = COMMENTS_DEPTH_MAX,
                type_ = ListingType.All,
                post_id = it,
                auth = account?.jwt,
            )
        }, {
            GetComments(
                max_depth = COMMENTS_DEPTH_MAX,
                type_ = ListingType.All,
                parent_id = it,
                auth = account?.jwt,
            )
        })

        val commentsOut = retrofitErrorHandler(
            api.getComments(
                commentsForm
                    .serializeToMap(),
            ),
        )

        return commentsOut.comments
    }

    suspend fun getCommunity(
        account: Account?,
        idOrName: Either<Int, String>,
    ): CommunityView {
        val form = idOrName.fold({ id ->
            GetCommunity(id = id, auth = account?.jwt)
        }, { name ->
            GetCommunity(name = name, auth = account?.jwt)
        })
        val out = retrofitErrorHandler(api.getCommunity(form = form.serializeToMap()))

        return out.community_view
    }

    fun getSite(): String =
        api.site
}