package com.idunnololz.summit.lemmy.multicommunity

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PostsDataSource
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import com.idunnololz.summit.util.dateStringToTs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MultiCommunityDataSource(
    private val instance: String,
    private val sources: List<LemmyListSource<PostView, SortType>>,
): PostsDataSource {

    companion object {
        private const val TAG = "MultiCommunityDataSource"
    }

    class Factory @Inject constructor(
        private val apiClient: AccountAwareLemmyClient,
    ) {
        fun create(
            instance: String,
            communities: List<CommunityRef.CommunityRefByName>,
        ): MultiCommunityDataSource {
            val sources = communities.map { communityRef ->
                LemmyListSource<PostView, SortType>(
                    apiClient,
                    { post.id },
                    SortType.Active,
                    { page: Int, sortOrder: SortType, limit: Int, force: Boolean ->
                        this.fetchPosts(
                            Either.Right(communityRef.getServerId()),
                            sortOrder,
                            ListingType.All,
                            page,
                            limit,
                            force,
                        )
                    }
                )
            }

            return MultiCommunityDataSource(instance, sources)
        }
    }

    data class Page(
        val posts: List<PostView>,
        val pageIndex: Int,
        val instance: String,
        val hasMore: Boolean,
    )

    private var pagesCache = mutableListOf<Page>()
    private var sortType: SortType? = null

    private val pagesContext = Dispatchers.Default.limitedParallelism(1)

    override suspend fun fetchPosts(
        sortType: SortType?,
        page: Int,
        force: Boolean,
        ): Result<List<PostView>> {
        if (force) {
            reset()
        }

        setSortType(sortType)

        return fetchPage(
            page,
            sortType ?: SortType.Active,
            ListingType.All,
            force,
        ).fold(
            onSuccess = {
                Result.success(it.posts)
            },
            onFailure = {
                if (it is EndReachedException) {
                    Result.success(listOf())
                } else {
                    Result.failure(it)
                }
            }
        )
    }

    private suspend fun fetchPage(
        pageIndex: Int,
        sortType: SortType,
        listingType: ListingType,
        force: Boolean,
    ): Result<Page> = withContext(pagesContext) a@{
        while (pagesCache.size <= pageIndex) {
            if (pagesCache.lastOrNull()?.hasMore == false) {
                return@a Result.failure(EndReachedException())
            }

            val nextPageResult = fetchNextPage()

            if (nextPageResult.isSuccess) {
                pagesCache.add(nextPageResult.getOrThrow())
            } else {
                return@a Result.failure(requireNotNull(nextPageResult.exceptionOrNull()))
            }
        }

        return@a Result.success(pagesCache[pageIndex])
    }

    private suspend fun fetchNextPage(): Result<Page> = withContext(pagesContext) a@{
        var hasMore = true
        val pageItems = mutableListOf<PostView>()

        while(true) {
            val results = sources.map { it.peekNextItem() }
            val error = results.firstOrNull { it.isFailure }

            if (error != null) {
                return@a Result.failure(requireNotNull(error.exceptionOrNull()))
            }
            val nextSource = sources.maxBy {
                val postView = it.peekNextItem().getOrThrow() ?: return@maxBy 0L

                if (postView.post.featured_local || postView.post.featured_community) {
                    return@maxBy Long.MAX_VALUE
                }

                when (sortType) {
                    SortType.Active -> postView.counts.hot_rank_active?.toLong() ?: 0L
                    SortType.Hot -> postView.counts.hot_rank?.toLong() ?: 0L
                    SortType.New -> dateStringToTs(postView.counts.published)
                    SortType.Old -> -dateStringToTs(postView.counts.published)
                    SortType.MostComments -> postView.counts.comments.toLong()
                    SortType.NewComments -> postView.counts.newest_comment_time.let {
                        dateStringToTs(it)
                    }
                    SortType.TopDay,
                    SortType.TopWeek,
                    SortType.TopMonth,
                    SortType.TopYear,
                    SortType.TopAll,
                    SortType.TopHour,
                    SortType.TopSixHour,
                    SortType.TopTwelveHour,
                    SortType.TopThreeMonths,
                    SortType.TopSixMonths,
                    SortType.TopNineMonths -> postView.counts.score.toLong()
                    else -> postView.counts.score.toLong()
                }
            }
            val nextItem = nextSource.peekNextItem().getOrNull()

            if (nextItem == null) {
                // no more items!
                hasMore = false
                break
            }

            Log.d(
                TAG,
                "Adding item ${nextItem.post.id} from source ${nextItem::class.java}",
            )

            pageItems.add(nextItem)

            // increment the max item
            nextSource.next()

            if (pageItems.size >= LemmyListSource.PAGE_SIZE) {
                break
            }
        }

        return@a Result.success(
            Page(
                pageItems,
                pagesCache.size,
                instance,
                hasMore,
            )
        )
    }

    private fun setSortType(sortType: SortType?) {
        if (this.sortType == sortType) {
            return
        }

        this.sortType = sortType
        sources.forEach {
            it.invalidate()
            it.sortOrder = sortType ?: SortType.Active
        }
        reset()
    }

    private fun reset() {
        sources.forEach {
            it.invalidate()
        }
        pagesCache.clear()
    }

    class EndReachedException : Exception()
}