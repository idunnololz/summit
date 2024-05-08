package com.idunnololz.summit.lemmy

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.api.utils.PostType
import com.idunnololz.summit.api.utils.getDominantType
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.filterLists.ContentFiltersManager
import com.idunnololz.summit.hidePosts.HiddenPostsManager
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.lemmy.multicommunity.MultiCommunityDataSource
import com.idunnololz.summit.lemmy.multicommunity.instance
import com.idunnololz.summit.util.retry
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@ViewModelScoped
class PostsRepository @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val apiClient: AccountAwareLemmyClient,
    private val postReadManager: PostReadManager,
    private val hiddenPostsManager: HiddenPostsManager,
    private val contentFiltersManager: ContentFiltersManager,
    private val singlePostsDataSourceFactory: SinglePostsDataSource.Factory,
    private val multiCommunityDataSourceFactory: MultiCommunityDataSource.Factory,
    private val accountManager: AccountManager,
) {
    companion object {
        private val TAG = PostsRepository::class.simpleName

        private const val DEFAULT_POSTS_PER_PAGE = 20
    }

    private data class PostData(
        val post: FetchedPost,
        val postPageIndexInternal: Int,
        val filterReason: FilterReason? = null,
    ) {
        val isFiltered = filterReason != null
    }

    private var currentDataSource: PostsDataSource = singlePostsDataSourceFactory.create(
        communityName = null,
        listingType = ListingType.All,
    )

    private var allPosts = mutableListOf<PostData>()
    private var seenPosts = mutableSetOf<String>()
    var persistentErrors: List<Exception> = listOf()

    private var communityRef: CommunityRef = CommunityRef.All()

    private var endReached = false

    private var currentPageInternal = 0
    private var postsPerPage = DEFAULT_POSTS_PER_PAGE

    var hideRead = false
    var hideReadCount = 0

    var showLinkPosts = true
    var showImagePosts = true
    var showVideoPosts = true
    var showTextPosts = true
    var showNsfwPosts = true
    var nsfwMode = false

    var showFilteredPosts = false

    private var consecutiveFilteredPostsByFilter = 0
    private var consecutiveFilteredPostsByType = 0

    var sortOrder = savedStateHandle.getStateFlow<CommunitySortOrder>(
        "PostsRepository.sortOrder",
        CommunitySortOrder.Active,
    )

    fun setSortOrder(sortOrder: CommunitySortOrder) {
        savedStateHandle["PostsRepository.sortOrder"] = sortOrder
    }

    suspend fun hideReadPosts(anchors: Set<PostId>, maxPage: Int): Result<PageResult> =
        updateStateMaintainingPosition({
            hideRead = true
        }, anchors, maxPage)

    suspend fun updateShowNsfwReadPosts(
        showNsfw: Boolean,
        anchors: Set<PostId>,
        maxPage: Int,
    ): Result<PageResult> = updateStateMaintainingPosition({
        this.showNsfwPosts = showNsfw
    }, anchors, maxPage)

    suspend fun updateStateMaintainingPosition(
        performChanges: PostsRepository.() -> Unit,
        anchors: Set<PostId>,
        maxPage: Int,
    ): Result<PageResult> {
        reset()

        this.performChanges()

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
            if (pageResult.posts.isEmpty() || anchors.isEmpty()) {
                break
            }

            // try to find our anchors
            val anchorsMatched = pageResult.posts.count {
                anchors.contains(it.fetchedPost.postView.post.id)
            }
            if (anchorsMatched > 0) {
                return result
            }

            curPage++
        }

        return getPage(0)
    }

    suspend fun getPage(pageIndex: Int, force: Boolean = false): Result<PageResult> {
        return withContext(Dispatchers.Default) {
            val startIndex = pageIndex * postsPerPage
            val endIndex = startIndex + postsPerPage

            if (force) {
                // delete all cached data for the given page
                val minPageInternal = allPosts
                    .slice(startIndex until min(endIndex, allPosts.size))
                    .minOfOrNull { it.postPageIndexInternal } ?: 0

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
                        sortType = sortOrder.value.toApiSortOrder(),
                        force = force,
                    )
                }

                if (hasMoreResult.isFailure) {
                    return@withContext Result.failure(
                        requireNotNull(hasMoreResult.exceptionOrNull()),
                    )
                } else {
                    hasMore = hasMoreResult.getOrThrow()
                    currentPageInternal++
                }

                if (!hasMore) {
                    endReached = true
                    break
                }
            }

            return@withContext Result.success(
                PageResult(
                    posts = allPosts
                        .slice(startIndex until min(endIndex, allPosts.size))
                        .map {
                            LocalPostView(
                                fetchedPost = transformPostWithLocalData(it.post),
                                filterReason = it.filterReason,
                            )
                        },
                    pageIndex = pageIndex,
                    instance = apiClient.instance,
                    hasMore = hasMore,
                ),
            )
        }
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
                is CommunityRef.ModeratedCommunities -> apiClient.instance
                is CommunityRef.AllSubscribed -> apiClient.instance
            }

    val apiInstance: String
        get() = apiClient.instance

    val apiInstanceFlow
        get() = apiClient.instanceFlow

    suspend fun setCommunity(communityRef: CommunityRef?) {
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

            is CommunityRef.AllSubscribed -> {
                val allAccounts = accountManager.getAccounts()

                currentDataSource = multiCommunityDataSourceFactory.createForSubscriptions(
                    apiInstance,
                    allAccounts,
                )
                postsPerPage = 15

                apiClient.defaultInstance()
            }

            is CommunityRef.CommunityRefByName -> {
                currentDataSource = singlePostsDataSourceFactory.create(
                    communityName = communityRef.getServerId(apiClient.instance),
                    listingType = ListingType.All,
                )
                postsPerPage = DEFAULT_POSTS_PER_PAGE

                apiClient.defaultInstance()
            }
            is CommunityRef.MultiCommunity -> {
                currentDataSource = multiCommunityDataSourceFactory.create(
                    apiClient.instance,
                    communityRef.communities,
                )
                postsPerPage = 15

                apiClient.defaultInstance()
            }
            is CommunityRef.ModeratedCommunities -> {
                currentDataSource = singlePostsDataSourceFactory.create(
                    communityName = null,
                    listingType = ListingType.ModeratorView,
                )
                postsPerPage = DEFAULT_POSTS_PER_PAGE

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

    suspend fun onAccountChanged() {
        reset()

        if (communityRef is CommunityRef.ModeratedCommunities) {
            // We need to reload the moderated community list on account switch
            setCommunity(communityRef)
        }
    }

    suspend fun onHiddenPostsChange() {
        val hiddenPosts = hiddenPostsManager.getHiddenPostEntries(apiInstance)
        allPosts.retainAll { !hiddenPosts.contains(it.post.postView.post.id) }
    }

    private fun transformPostWithLocalData(fetchedPost: FetchedPost): FetchedPost {
        val accountInstance = fetchedPost.source.instance ?: apiInstance
        val localRead = postReadManager.isPostRead(accountInstance, fetchedPost.postView.post.id)
        if (localRead && !fetchedPost.postView.read) {
            return fetchedPost.copy(postView = fetchedPost.postView.copy(read = true))
        }
        return fetchedPost
    }

    /**
     * @return true if there might be more posts to fetch
     */
    private suspend fun fetchPage(
        pageIndex: Int,
        sortType: SortType,
        force: Boolean,
    ): Result<Boolean> {
        val currentDataSource = currentDataSource
        val newPostResults = currentDataSource.fetchPosts(sortType, pageIndex, force)
        val hiddenPosts = hiddenPostsManager.getHiddenPostEntries(apiInstance)

        if (currentDataSource is MultiCommunityDataSource) {
            persistentErrors = currentDataSource.getPersistentErrors()
        }

        return newPostResults.fold(
            onSuccess = { newPosts ->
                if (newPosts.isNotEmpty()) {
                    if (newPosts.first().postView.community.nsfw &&
                        communityRef is CommunityRef.CommunityRefByName &&
                        !showNsfwPosts &&
                        !nsfwMode
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
                Log.d(TAG, "fetchPage() error: $it")
                Result.failure(it)
            },
        )
    }

    private fun addPosts(
        newPosts: List<FetchedPost>,
        pageIndex: Int,
        hiddenPosts: Set<PostId>,
        force: Boolean,
    ) {
        for (fetchedPost in newPosts) {
            val post = fetchedPost.postView
            val uniquePostKey = post.getUniqueKey()
            var filterReason: FilterReason? = null
            val instance = fetchedPost.source.instance ?: apiInstance

            if (hideRead && (post.read || postReadManager.isPostRead(instance, post.post.id))) {
                hideReadCount++
                continue
            }
            if (!showNsfwPosts && post.post.nsfw && !nsfwMode) {
                if (showFilteredPosts) {
                    filterReason = FilterReason.Nsfw
                } else {
                    continue
                }
            }
            val postType = post.getDominantType()
            if (!showLinkPosts && postType == PostType.Link) {
                if (showFilteredPosts) {
                    filterReason = FilterReason.Link
                } else {
                    consecutiveFilteredPostsByType++
                    continue
                }
            }
            if (!showImagePosts && postType == PostType.Image) {
                if (showFilteredPosts) {
                    filterReason = FilterReason.Image
                } else {
                    consecutiveFilteredPostsByType++
                    continue
                }
            }
            if (!showVideoPosts && postType == PostType.Video) {
                if (showFilteredPosts) {
                    filterReason = FilterReason.Video
                } else {
                    consecutiveFilteredPostsByType++
                    continue
                }
            }
            if (!showTextPosts && postType == PostType.Text) {
                if (showFilteredPosts) {
                    filterReason = FilterReason.Text
                } else {
                    consecutiveFilteredPostsByType++
                    continue
                }
            }
            if (hiddenPosts.contains(post.post.id)) {
                continue
            }
            if (contentFiltersManager.testPostView(post)) {
                if (showFilteredPosts) {
                    filterReason = FilterReason.Custom
                } else {
                    consecutiveFilteredPostsByFilter++
                    continue
                }
            }

            consecutiveFilteredPostsByFilter = 0
            consecutiveFilteredPostsByType = 0

            if (seenPosts.add(uniquePostKey)) {
                allPosts.add(
                    PostData(
                        post = fetchedPost,
                        postPageIndexInternal = pageIndex,
                        filterReason = filterReason,
                    ),
                )
            }
        }
    }

    private fun deleteFromPage(minPageInternal: Int) {
        allPosts.retainAll {
            val keep = it.postPageIndexInternal < minPageInternal
            if (!keep) {
                seenPosts.remove(it.post.postView.getUniqueKey())
            }
            keep
        }

        currentPageInternal = minPageInternal

        Log.d(TAG, "Deleted pages $minPageInternal and beyond. Posts left: ${allPosts.size}")
    }

    fun update(posts: List<LocalPostView>): List<LocalPostView> {
        return posts.map {
            it.copy(
                fetchedPost = transformPostWithLocalData(it.fetchedPost),
            )
        }
    }

    fun clearHideRead() {
        hideRead = false
        hideReadCount = 0
    }

    fun addSeenPosts(posts: List<PostView>) {
        seenPosts.addAll(posts.map { it.getUniqueKey() })
    }

    interface Factory {
        fun create(instance: String): PostsRepository
    }

    class PageResult(
        val posts: List<LocalPostView>,
        val pageIndex: Int,
        val instance: String,
        val hasMore: Boolean,
    )
}

class LoadNsfwCommunityWhenNsfwDisabled() : RuntimeException()
class FilterTooAggressiveException() : RuntimeException()
class ContentTypeFilterTooAggressiveException() : RuntimeException()
