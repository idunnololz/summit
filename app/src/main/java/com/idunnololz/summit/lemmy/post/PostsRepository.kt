package com.idunnololz.summit.lemmy.post

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.Community
import com.idunnololz.summit.lemmy.CommunitySortOrder
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlin.math.min

@ViewModelScoped
class PostsRepository @Inject constructor(
    private val apiClient: LemmyApiClient,
) {
    companion object {
        private val TAG = PostsRepository::class.simpleName

        private val DEFAULT_LEMMY_INSTANCES = listOf(
            "beehaw.org",
            "feddit.de",
            "feddit.it",
            "lemmy.ca",
            "lemmy.ml",
            "lemmy.one",
            "lemmy.world",
            "lemmygrad.ml",
            "midwest.social",
            "mujico.org",
            "sh.itjust.works",
            "slrpnk.net",
            "sopuli.xyz",
            "szmer.info",
        )

        private const val POSTS_PER_PAGE = 20
    }

    private data class PostData(
        val post: PostView,
        val postPageIndexInternal: Int,
    )

    private val allPosts = mutableListOf<PostData>()
    private val seenPosts = mutableSetOf<String>()

    private var community: Community = Community.All()

    private var endReached = false

    private var currentPageInternal = 1

    var sortOrder: CommunitySortOrder = CommunitySortOrder.Active

    suspend fun getPage(pageIndex: Int, account: Account?, force: Boolean = false): PageResult {
        val startIndex = pageIndex * POSTS_PER_PAGE
        val endIndex = startIndex + POSTS_PER_PAGE

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

            hasMore = fetchPage(currentPageInternal, account)
            currentPageInternal++

            if (!hasMore) {
                endReached = true
                break
            }
        }

        return PageResult(
            allPosts.slice(startIndex until min(endIndex, allPosts.size)).map { it.post },
            hasMore = hasMore
        )
    }

    fun getSite(): String =
        apiClient.getSite()

    fun setCommunity(community: Community?) {
        this.community = community ?: Community.All()
        reset()
    }

    private fun reset() {
        currentPageInternal = 1

        allPosts.clear()
        seenPosts.clear()
    }

    /**
     * @return true if there might be more posts to fetch
     */
    private suspend fun fetchPage(
        pageIndex: Int,
        account: Account?,
        communityIdOrName: Either<Int, String>? = null,
    ): Boolean {
        val newPosts = if (account != null) {
//            apiClient.changeInstance(account.instance)

            apiClient.fetchPosts(
                account = account,
                communityIdOrName = communityIdOrName,
                sortType = SortType.values()[account.defaultSortType],
                listingType = ListingType.values()[account.defaultListingType],
                page = pageIndex
            )
        } else {
            Log.d("jerboa", "Fetching posts for anonymous user")
//            apiClient.changeInstance(DEFAULT_INSTANCE)

            apiClient.fetchPosts(
                account = null,
                communityIdOrName = communityIdOrName,
                sortType = SortType.Active,
                listingType = ListingType.Local,
                page = pageIndex
            )
        }

        addPosts(newPosts, pageIndex)

        return newPosts.isNotEmpty()
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
        val hasMore: Boolean,
    )
}