package com.idunnololz.summit.lemmy

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.retry
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.min

@ViewModelScoped
class PostsRepository @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val postReadManager: PostReadManager,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
) {
    companion object {
        private val TAG = PostsRepository::class.simpleName

        private const val POSTS_PER_PAGE = 20
    }

    private data class PostData(
        val post: PostView,
        val postPageIndexInternal: Int,
    )

    private val coroutineScope = coroutineScopeFactory.create()

    private var allPosts = mutableListOf<PostData>()
    private var seenPosts = mutableSetOf<String>()

    private var communityRef: CommunityRef = CommunityRef.All()

    private var endReached = false

    private var currentPageInternal = 1
    var hideRead = false

    var sortOrder: CommunitySortOrder = CommunitySortOrder.Active
        set(value) {
            field = value
            reset()
        }

    init {
        coroutineScope.launch {
            accountInfoManager.currentFullAccount.collect {
                if (it != null) {
                    sortOrder = it
                        .accountInfo
                        .miscAccountInfo
                        ?.defaultCommunitySortType
                        ?.toSortOrder()
                        ?: return@collect
                }
            }
        }
    }

    suspend fun hideReadPosts(anchors: Set<PostId>, maxPage: Int): Result<PageResult> {
        reset()
        hideRead = true

        val anchorsSet = anchors

        var curPage = 0
        while (true) {
            if (curPage > maxPage) {
                break
            }

            val result = getPage(curPage, force = true)
            if (result.isFailure) {
                return result
            }

            val pageResult = result.getOrNull() ?: break
            if (pageResult.posts.isEmpty() || anchorsSet.isEmpty()) {
                break
            }

            // try to find our anchors
            val anchorsMatched = pageResult.posts.count { anchorsSet.contains(it.post.id) }
            if (anchorsMatched > 0) {
                return result
            }

            curPage++
        }

        return getPage(0)
    }

    suspend fun getPage(pageIndex: Int, force: Boolean = false): Result<PageResult> {
        val startIndex = pageIndex * POSTS_PER_PAGE
        val endIndex = startIndex + POSTS_PER_PAGE

        val communityRef = communityRef

        if (force) {
            // delete all cached data for the given page
            val minPageInternal = allPosts
                .slice(startIndex until min(endIndex, allPosts.size))
                .map { it.postPageIndexInternal }
                .minOrNull() ?: 1

            deleteFromPage(minPageInternal)
            endReached = false
        }

        var hasMore = true

        while (true) {
            if (allPosts.size >= endIndex) {
                break
            }

            if (endReached) {
                hasMore = false
                break
            }

            val hasMoreResult = retry {
                when (communityRef) {
                    is CommunityRef.All ->
                        fetchPage(
                            pageIndex = currentPageInternal,
                            communityIdOrName = null,
                            sortType = sortOrder.toApiSortOrder(),
                            listingType = ListingType.All,
                            force = force,
                        )
                    is CommunityRef.Local ->
                        fetchPage(
                            pageIndex = currentPageInternal,
                            communityIdOrName = null,
                            sortType = sortOrder.toApiSortOrder(),
                            listingType = ListingType.Local,
                            force = force,
                        )
                    is CommunityRef.CommunityRefByName ->
                        fetchPage(
                            pageIndex = currentPageInternal,
                            communityIdOrName = Either.Right(communityRef.getServerId()),
                            sortType = sortOrder.toApiSortOrder(),
                            listingType = ListingType.All,
                            force = force,
                        )
                    is CommunityRef.Subscribed ->
                        fetchPage(
                            pageIndex = currentPageInternal,
                            communityIdOrName = null,
                            sortType = sortOrder.toApiSortOrder(),
                            listingType = ListingType.Subscribed,
                            force = force,
                        )
                }
            }

            if (hasMoreResult.isFailure) {
                return Result.failure(requireNotNull(hasMoreResult.exceptionOrNull()))
            } else {
                hasMore = hasMoreResult.getOrThrow()
                currentPageInternal++
            }


            if (!hasMore) {
                endReached = true
                break
            }
        }

        return Result.success(
            PageResult(
                posts = allPosts
                    .slice(startIndex until min(endIndex, allPosts.size))
                    .map {
                        transformPostWithLocalData(it.post)
                    },
                pageIndex = pageIndex,
                instance = apiClient.instance,
                hasMore = hasMore
            )
        )
    }

    val communityInstance: String
        get() =
            when (val communityRef = communityRef) {
                is CommunityRef.All -> apiClient.instance
                is CommunityRef.CommunityRefByName -> communityRef.instance ?: apiClient.instance
                is CommunityRef.Local -> apiClient.instance
                is CommunityRef.Subscribed -> communityRef.instance ?: apiClient.instance
            }

    val accountInstance: String
        get() = apiClient.instance

    fun setCommunity(communityRef: CommunityRef?) {
        this.communityRef = communityRef ?: CommunityRef.All()

        when (communityRef) {
            is CommunityRef.Local -> {
                if (communityRef.instance != null) {
                    apiClient.changeInstance(communityRef.instance)
                } else {
                    apiClient.defaultInstance()
                }
            }

            is CommunityRef.All -> {
                if (communityRef.instance != null) {
                    apiClient.changeInstance(communityRef.instance)
                } else {
                    apiClient.defaultInstance()
                }
            }

            is CommunityRef.Subscribed -> {
                apiClient.defaultInstance()
            }

            is CommunityRef.CommunityRefByName,
            null -> {
                apiClient.defaultInstance()
            }
        }

        reset()
    }

    suspend fun resetCacheForCommunity() {
        reset()

        val apiClient = apiClient

        withContext(Dispatchers.Main) {
            val urlIterator = apiClient.apiClient.okHttpClient.cache?.urls() ?: return@withContext
            while (urlIterator.hasNext()) {

                if (urlIterator.next().startsWith(
                        "https://${apiClient.instance}/api/v3/post/list",
                        ignoreCase = true
                    )) {

                    urlIterator.remove()
                }
            }
        }
    }

    fun reset() {
        currentPageInternal = 1
        endReached = false

        allPosts = mutableListOf()
        seenPosts = mutableSetOf()
    }

    private fun transformPostWithLocalData(postView: PostView): PostView {
        val accountInstance = accountInstance
        val localRead = postReadManager.isPostRead(accountInstance, postView.post.id)
        if (localRead && !postView.read) {
            return postView.copy(read = true)
        }
        return postView
    }

    /**
     * @return true if there might be more posts to fetch
     */
    private suspend fun fetchPage(
        pageIndex: Int,
        communityIdOrName: Either<Int, String>? = null,
        sortType: SortType,
        listingType: ListingType,
        force: Boolean,
    ): Result<Boolean> {
        val newPosts =
            apiClient.fetchPosts(
                communityIdOrName = communityIdOrName,
                sortType = sortType,
                listingType = listingType,
                page = pageIndex,
                limit = 20,
                force = force,
            )

        return newPosts.fold(
            onSuccess = { newPosts ->
                Log.d(TAG, "Fetched ${newPosts.size} posts.")
                addPosts(newPosts, pageIndex)
                Result.success(newPosts.isNotEmpty())
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    private fun addPosts(newPosts: List<PostView>, pageIndex: Int,) {
        for (post in newPosts) {
            val uniquePostKey = post.getUniqueKey()
            if (hideRead && (post.read || postReadManager.isPostRead(accountInstance, post.post.id))) {
                continue
            }

            if (seenPosts.add(uniquePostKey)) {
                allPosts.add(PostData(post, pageIndex))
            }
        }
    }

    private fun deleteFromPage(minPageInternal: Int) {
        allPosts.retainAll {
            val keep = it.postPageIndexInternal < minPageInternal
            if (!keep) {
                seenPosts.remove(it.post.getUniqueKey())
            }
            keep
        }

        currentPageInternal = minPageInternal

        Log.d(TAG, "Deleted pages ${minPageInternal} and beyond. Posts left: ${allPosts.size}")
    }

    fun update(posts: List<PostView>): List<PostView> {
        return posts.map {
            transformPostWithLocalData(it)
        }
    }

    fun clearHideRead() {
        hideRead = false
    }

    interface Factory {
        fun create(instance: String): PostsRepository
    }

    class PageResult(
        val posts: List<PostView>,
        val pageIndex: Int,
        val instance: String,
        val hasMore: Boolean,
    )
}
