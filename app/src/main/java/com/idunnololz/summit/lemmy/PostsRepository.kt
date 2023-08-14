package com.idunnololz.summit.lemmy

import android.util.Log
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.api.utils.PostType
import com.idunnololz.summit.api.utils.getDominantType
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.filterLists.ContentFiltersManager
import com.idunnololz.summit.hidePosts.HiddenPostsManager
import com.idunnololz.summit.lemmy.multicommunity.MultiCommunityDataSource
import com.idunnololz.summit.lemmy.utils.toVotableRef
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.retry
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val accountActionsManager: AccountActionsManager,
    private val hiddenPostsManager: HiddenPostsManager,
    private val contentFiltersManager: ContentFiltersManager,
    private val preferences: Preferences,

    private val singlePostsDataSourceFactory: SinglePostsDataSource.Factory,
    private val multiCommunityDataSourceFactory: MultiCommunityDataSource.Factory,
) {
    companion object {
        private val TAG = PostsRepository::class.simpleName

        private const val DEFAULT_POSTS_PER_PAGE = 20
    }

    private data class PostData(
        val post: PostView,
        val postPageIndexInternal: Int,
    )

    private val coroutineScope = coroutineScopeFactory.create()
    private val allPostsContext = Dispatchers.IO.limitedParallelism(1)

    private var currentDataSource: PostsDataSource = singlePostsDataSourceFactory.create(
        communityName = null,
        listingType = ListingType.All,
    )

    private var allPosts = mutableListOf<PostData>()
    private var seenPosts = mutableSetOf<String>()

    private var communityRef: CommunityRef = CommunityRef.All()

    private var endReached = false

    private var currentPageInternal = 0
    private var postsPerPage = DEFAULT_POSTS_PER_PAGE

    var hideRead = false

    var showLinkPosts = true
    var showImagePosts = true
    var showVideoPosts = true
    var showTextPosts = true
    var showNsfwPosts = true

    private var consecutiveFilteredPostsByFilter = 0
    private var consecutiveFilteredPostsByType = 0

    var sortOrder: CommunitySortOrder = CommunitySortOrder.Active
        set(value) {
            field = value

            coroutineScope.launch {
                sortOrderFlow.value = value
            }

            reset()
        }
    val sortOrderFlow = MutableStateFlow<CommunitySortOrder>(sortOrder)

    init {
        coroutineScope.launch {
            accountInfoManager.currentFullAccount.collect { fullAccount ->
                if (fullAccount != null) {
                    sortOrder =
                        preferences.defaultCommunitySortOrder
                            ?: fullAccount
                                .accountInfo
                                .miscAccountInfo
                                ?.defaultCommunitySortType
                                ?.toSortOrder()
                            ?: return@collect
                }
            }
        }
    }

    suspend fun hideReadPosts(anchors: Set<PostId>, maxPage: Int): Result<PageResult> =
        updateStateMaintainingPosition({
            hideRead = true
        }, anchors, maxPage,)

    suspend fun updateShowNsfwReadPosts(
        showNsfw: Boolean,
        anchors: Set<PostId>,
        maxPage: Int,
    ): Result<PageResult> =
        updateStateMaintainingPosition({
            this.showNsfwPosts = showNsfw
        }, anchors, maxPage,)

    suspend fun updateStateMaintainingPosition(
        performChanges: PostsRepository.() -> Unit,
        anchors: Set<PostId>,
        maxPage: Int,
    ): Result<PageResult> {
        reset()

        this.performChanges()

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
        val startIndex = pageIndex * postsPerPage
        val endIndex = startIndex + postsPerPage

        val communityRef = communityRef

        if (force) {
            // delete all cached data for the given page
            val minPageInternal = allPosts
                .slice(startIndex until min(endIndex, allPosts.size))
                .map { it.postPageIndexInternal }
                .minOrNull() ?: 0

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
                fetchPage(
                    pageIndex = currentPageInternal,
                    sortType = sortOrder.toApiSortOrder(),
                    force = force,
                )
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
                hasMore = hasMore,
            ),
        )
    }

    val communityInstance: String
        get() =
            when (val communityRef = communityRef) {
                is CommunityRef.All -> apiClient.instance
                is CommunityRef.CommunityRefByName -> communityRef.instance ?: apiClient.instance
                is CommunityRef.Local -> apiClient.instance
                is CommunityRef.Subscribed -> communityRef.instance ?: apiClient.instance
                is CommunityRef.MultiCommunity ->
                    communityRef.communities.firstOrNull()?.instance ?: apiClient.instance
            }

    val apiInstance: String
        get() = apiClient.instance

    fun setCommunity(communityRef: CommunityRef?) {
        this.communityRef = communityRef ?: CommunityRef.All()

        when (communityRef) {
            is CommunityRef.Local -> {
                currentDataSource = singlePostsDataSourceFactory.create(
                    communityName = null,
                    listingType = ListingType.Local,
                )
                postsPerPage = DEFAULT_POSTS_PER_PAGE

                if (communityRef.instance != null) {
                    apiClient.changeInstance(communityRef.instance)
                } else {
                    apiClient.defaultInstance()
                }
            }

            is CommunityRef.All -> {
                currentDataSource = singlePostsDataSourceFactory.create(
                    communityName = null,
                    listingType = ListingType.All,
                )
                postsPerPage = DEFAULT_POSTS_PER_PAGE

                if (communityRef.instance != null) {
                    apiClient.changeInstance(communityRef.instance)
                } else {
                    apiClient.defaultInstance()
                }
            }

            is CommunityRef.Subscribed -> {
                currentDataSource = singlePostsDataSourceFactory.create(
                    communityName = null,
                    listingType = ListingType.Subscribed,
                )
                postsPerPage = DEFAULT_POSTS_PER_PAGE

                apiClient.defaultInstance()
            }

            is CommunityRef.CommunityRefByName -> {
                currentDataSource = singlePostsDataSourceFactory.create(
                    communityName = communityRef.getServerId(),
                    listingType = ListingType.All,
                )
                postsPerPage = DEFAULT_POSTS_PER_PAGE

                apiClient.defaultInstance()
            }
            is CommunityRef.MultiCommunity -> {
                currentDataSource = multiCommunityDataSourceFactory.create(
                    apiClient.instance,
                    communityRef.communities
                )
                postsPerPage = 15

                apiClient.defaultInstance()
            }
            null,
            -> {
                currentDataSource = singlePostsDataSourceFactory.create(
                    communityName = null,
                    listingType = ListingType.All,
                )
                postsPerPage = DEFAULT_POSTS_PER_PAGE

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
                        ignoreCase = true,
                    )
                ) {
                    urlIterator.remove()
                }
            }
        }
    }

    fun reset() {
        currentPageInternal = 0
        endReached = false

        allPosts = mutableListOf()
        seenPosts = mutableSetOf()
    }

    suspend fun onHiddenPostsChange() {
        val hiddenPosts = hiddenPostsManager.getHiddenPostEntries(apiInstance)
        allPosts.retainAll { !hiddenPosts.contains(it.post.post.id) }
    }

    private fun transformPostWithLocalData(postView: PostView): PostView {
        val accountInstance = apiInstance
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
        sortType: SortType,
        force: Boolean,
    ): Result<Boolean> {
        val newPosts = currentDataSource.fetchPosts(sortType, pageIndex, force)
        val hiddenPosts = hiddenPostsManager.getHiddenPostEntries(apiInstance)

        return newPosts.fold(
            onSuccess = { newPosts ->
                if (newPosts.isNotEmpty()) {
                    if (newPosts.first().community.nsfw &&
                        communityRef is CommunityRef.CommunityRefByName &&
                        !showNsfwPosts
                    ) {
                        return@fold Result.failure(LoadNsfwCommunityWhenNsfwDisabled())
                    }
                }

                Log.d(TAG, "Fetched ${newPosts.size} posts.")
                addPosts(newPosts, pageIndex, hiddenPosts, force)

                if (consecutiveFilteredPostsByFilter > 20) {
                    Result.failure(FilterTooAggressiveException())
                } else if (consecutiveFilteredPostsByType > 20) {
                    Result.failure(ContentTypeFilterTooAggressiveException())
                } else {
                    Result.success(newPosts.isNotEmpty())
                }
            },
            onFailure = {
                Result.failure(it)
            },
        )
    }

    private fun addPosts(
        newPosts: List<PostView>,
        pageIndex: Int,
        hiddenPosts: Set<PostId>,
        force: Boolean,
    ) {
        for (post in newPosts) {
            val uniquePostKey = post.getUniqueKey()

            if (force) {
                accountActionsManager.setScore(post.toVotableRef(), post.counts.score)
            }
            if (hideRead && (post.read || postReadManager.isPostRead(apiInstance, post.post.id))) {
                continue
            }
            if (!showNsfwPosts && post.post.nsfw) {
                continue
            }
            val postType = post.getDominantType()
            if (!showLinkPosts && postType == PostType.Link) {
                consecutiveFilteredPostsByType++
                continue
            }
            if (!showImagePosts && postType == PostType.Image) {
                consecutiveFilteredPostsByType++
                continue
            }
            if (!showVideoPosts && postType == PostType.Video) {
                consecutiveFilteredPostsByType++
                continue
            }
            if (!showTextPosts && postType == PostType.Text) {
                consecutiveFilteredPostsByType++
                continue
            }
            if (hiddenPosts.contains(post.post.id)) {
                continue
            }
            if (contentFiltersManager.testPostView(post)) {
                consecutiveFilteredPostsByFilter++
                continue
            }

            consecutiveFilteredPostsByFilter = 0
            consecutiveFilteredPostsByType = 0

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

        Log.d(TAG, "Deleted pages $minPageInternal and beyond. Posts left: ${allPosts.size}")
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

class LoadNsfwCommunityWhenNsfwDisabled() : RuntimeException()
class FilterTooAggressiveException() : RuntimeException()
class ContentTypeFilterTooAggressiveException() : RuntimeException()
