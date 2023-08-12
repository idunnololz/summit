package com.idunnololz.summit.lemmy.multicommunity

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.ContentTypeFilterTooAggressiveException
import com.idunnololz.summit.lemmy.FilterTooAggressiveException
import com.idunnololz.summit.lemmy.LoadNsfwCommunityWhenNsfwDisabled
import com.idunnololz.summit.lemmy.PostsRepository
import com.idunnololz.summit.lemmy.inbox.InboxRepository
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MultiCommunityDataSource(
    private val instance: String,
    private val sources: List<LemmyListSource<PostView, SortType>>,
) {

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
                            Either.Right(communityRef.name),
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

    private val pagesContext = Dispatchers.Default.limitedParallelism(1)

    /**
     * @return true if there might be more posts to fetch
     */
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
                it.peekNextItem().getOrThrow()?.counts?.score ?: 0
//            when (sortType) {
//                SortType.Active ->
//                SortType.Hot -> TODO()
//                SortType.New -> TODO()
//                SortType.Old -> TODO()
//                SortType.TopDay -> TODO()
//                SortType.TopWeek -> TODO()
//                SortType.TopMonth -> TODO()
//                SortType.TopYear -> TODO()
//                SortType.TopAll -> TODO()
//                SortType.MostComments -> TODO()
//                SortType.NewComments -> TODO()
//                SortType.TopHour -> TODO()
//                SortType.TopSixHour -> TODO()
//                SortType.TopTwelveHour -> TODO()
//                SortType.TopThreeMonths -> TODO()
//                SortType.TopSixMonths -> TODO()
//                SortType.TopNineMonths -> TODO()
//            }
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

    class EndReachedException : Exception()
}