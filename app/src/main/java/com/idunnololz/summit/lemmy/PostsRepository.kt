package com.idunnololz.summit.lemmy

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.util.retry
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlin.math.min

@ViewModelScoped
class PostsRepository @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
) {
    companion object {
        private val TAG = PostsRepository::class.simpleName

        private const val POSTS_PER_PAGE = 20
    }

    private data class PostData(
        val post: PostView,
        val postPageIndexInternal: Int,
    )

    private val allPosts = mutableListOf<PostData>()
    private val seenPosts = mutableSetOf<String>()

    private var communityRef: CommunityRef = CommunityRef.All()

    private var endReached = false

    private var currentPageInternal = 1

    var sortOrder: CommunitySortOrder = CommunitySortOrder.Active
        set(value) {
            field = value
            reset()
        }

    suspend fun getPage(pageIndex: Int, force: Boolean = false): Result<PageResult> {
        val startIndex = pageIndex * POSTS_PER_PAGE
        val endIndex = startIndex + POSTS_PER_PAGE

        val communityRef = communityRef

        if (force) {
            // delete all cached data for the given page
            val minPageInteral = allPosts
                .slice(startIndex until min(endIndex, allPosts.size))
                .map { it.postPageIndexInternal }
                .minOrNull() ?: 1

            deleteFromPage(minPageInteral)

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
                        )
                    is CommunityRef.CommunityRefByObj ->
                        fetchPage(
                            pageIndex = currentPageInternal,
                            communityIdOrName = Either.Left(communityRef.community.id),
                            sortType = sortOrder.toApiSortOrder(),
                            listingType = ListingType.All,
                        )
                    is CommunityRef.Local ->
                        fetchPage(
                            pageIndex = currentPageInternal,
                            communityIdOrName = null,
                            sortType = sortOrder.toApiSortOrder(),
                            listingType = ListingType.Local,
                        )
                    is CommunityRef.CommunityRefByName ->
                        fetchPage(
                            pageIndex = currentPageInternal,
                            communityIdOrName = Either.Right(communityRef.name),
                            sortType = sortOrder.toApiSortOrder(),
                            listingType = ListingType.All,
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
                    .map { it.post },
                instance = apiClient.instance,
                hasMore = hasMore
            )
        )
    }

    val instance: String
        get() = apiClient.instance

    fun setCommunity(communityRef: CommunityRef?) {
        this.communityRef = communityRef ?: CommunityRef.All()

        if (communityRef is CommunityRef.Local) {
            if (communityRef.site != null) {
                apiClient.changeInstance(communityRef.site)
            } else {
                apiClient.defaultInstance()
            }
        } else if (communityRef is CommunityRef.All) {
            if (communityRef.site != null) {
                apiClient.changeInstance(communityRef.site)
            } else {
                apiClient.defaultInstance()
            }
        }

        reset()
    }

    fun reset() {
        currentPageInternal = 1

        allPosts.clear()
        seenPosts.clear()
    }

    /**
     * @return true if there might be more posts to fetch
     */
    private suspend fun fetchPage(
        pageIndex: Int,
        communityIdOrName: Either<Int, String>? = null,
        sortType: SortType,
        listingType: ListingType,
    ): Result<Boolean> {
        val newPosts =
            apiClient.fetchPosts(
                communityIdOrName = communityIdOrName,
                sortType = sortType,
                listingType = listingType,
                page = pageIndex,
                limit = 20,
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
        newPosts.forEach {
            val uniquePostKey = it.getUniqueKey()
            if (seenPosts.add(uniquePostKey)) {
                allPosts.add(PostData(it, pageIndex))
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

    interface Factory {
        fun create(instance: String): PostsRepository
    }

    class PageResult(
        val posts: List<PostView>,
        val instance: String,
        val hasMore: Boolean,
    )
}