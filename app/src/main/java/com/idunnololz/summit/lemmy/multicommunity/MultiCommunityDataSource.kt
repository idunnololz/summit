package com.idunnololz.summit.lemmy.multicommunity

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PostsDataSource
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.getActiveRank
import com.idunnololz.summit.lemmy.utils.getControversialRank
import com.idunnololz.summit.lemmy.utils.getHotRank
import com.idunnololz.summit.lemmy.utils.getScaledRank
import com.idunnololz.summit.util.dateStringToTs
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class MultiCommunityDataSource(
    private val instance: String,
    private val sources: List<LemmyListSource<FetchedPost, SortType>>,
    private val apiClient: AccountAwareLemmyClient,
) : PostsDataSource {

    companion object {
        private const val TAG = "MultiCommunityDataS"

        private const val DEFAULT_PAGE_SIZE = 10

        /**
         * Maximum number of communities that can be added into one multi-community.
         */
        const val MULTI_COMMUNITY_DATA_SOURCE_LIMIT = 30
    }

    class Factory @Inject constructor(
        private val apiClient: AccountAwareLemmyClient,
        private val apiClientFactory: LemmyApiClient.Factory,
    ) {
        fun create(
            instance: String,
            communities: List<CommunityRef.CommunityRefByName>,
        ): MultiCommunityDataSource {
            val sources = communities.map { communityRef ->
                LemmyListSource(
                    { postView.post.id },
                    SortType.Active,
                    { page: Int, sortOrder: SortType, limit: Int, force: Boolean ->
                        apiClient
                            .fetchPosts(
                                communityIdOrName =
                                Either.Right(communityRef.getServerId(apiClient.instance)),
                                sortType = sortOrder,
                                listingType = ListingType.All,
                                page = page,
                                limit = limit,
                                force = force,
                            )
                            .map {
                                it.map {
                                    FetchedPost(
                                        it,
                                        Source.StandardSource(),
                                    )
                                }
                            }
                    },
                    DEFAULT_PAGE_SIZE,
                    communityRef,
                )
            }.take(MULTI_COMMUNITY_DATA_SOURCE_LIMIT)

            return MultiCommunityDataSource(instance, sources, apiClient)
        }

        fun createForSubscriptions(
            currentInstance: String,
            accounts: List<Account>,
        ): MultiCommunityDataSource {
            val sources = accounts.map { account ->
                val apiClient = apiClientFactory.create()
                apiClient.changeInstance(account.instance)
                LemmyListSource(
                    { postView.post.id },
                    SortType.Active,
                    { page: Int, sortOrder: SortType, limit: Int, force: Boolean ->
                        apiClient
                            .fetchPosts(
                                account = account,
                                communityIdOrName = null,
                                sortType = sortOrder,
                                listingType = ListingType.Subscribed,
                                page = page,
                                limit = limit,
                                force = force,
                            )
                            .map {
                                it.map {
                                    FetchedPost(
                                        it,
                                        Source.AccountSource(
                                            name = account.name,
                                            id = account.id,
                                            instance = account.instance,
                                        ),
                                    )
                                }
                            }
                    },
                    DEFAULT_PAGE_SIZE,
                    account,
                )
            }.take(MULTI_COMMUNITY_DATA_SOURCE_LIMIT)

            return MultiCommunityDataSource(currentInstance, sources, apiClient)
        }
    }

    data class Page(
        val posts: List<FetchedPost>,
        val pageIndex: Int,
        val instance: String,
        val hasMore: Boolean,
    )

    private var pagesCache = mutableListOf<Page>()
    private var sortType: SortType? = null
    private var communityNotFoundOnInstance = mutableSetOf<CommunityRef.CommunityRefByName>()

    private val communityToMau = mutableMapOf<String, Int>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pagesContext = Dispatchers.Default.limitedParallelism(1)

    private val validSources
        get() = sources.filter { !communityNotFoundOnInstance.contains(it.source) }

    override suspend fun fetchPosts(
        sortType: SortType?,
        page: Int,
        force: Boolean,
    ): Result<List<FetchedPost>> = withContext(Dispatchers.Default) {
        if (force) {
            reset()
        }

        setSortType(sortType)

        // prefetch if needed
        val prefetchJobs = validSources.map {
            async { it.peekNextItem() }
        }
        prefetchJobs.forEach {
            it.await()
        }

        fetchPage(
            page,
        ).fold(
            onSuccess = {
                Result.success(
                    it.posts,
                )
            },
            onFailure = {
                if (it is EndReachedException) {
                    Result.success(listOf())
                } else {
                    Result.failure(it)
                }
            },
        )
    }

    fun getPersistentErrors(): List<Exception> {
        return communityNotFoundOnInstance.map {
            CommunityNotFoundException(instance, it)
        }
    }

    val sourcesCount: Int
        get() = sources.size

    private suspend fun fetchPage(pageIndex: Int): Result<Page> = withContext(pagesContext) a@{
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
        val pageItems = mutableListOf<FetchedPost>()

        while (true) {
            val validSources = validSources
            var sourceToResult = validSources.map { it to it.peekNextItem() }
            val sourceAndError = sourceToResult
                .firstOrNull { (_, result) -> result.isFailure }

            if (sourceAndError != null) {
                val exception = requireNotNull(sourceAndError.second.exceptionOrNull())
                if (exception is ClientApiException && exception.errorCode == 404) {
                    communityNotFoundOnInstance.add(
                        sourceAndError.first.source as CommunityRef.CommunityRefByName,
                    )
                } else {
                    return@a Result.failure(exception)
                }
            }
            sourceToResult = sourceToResult.filter { it.second.isSuccess }

            val nextSourceAndResult = sourceToResult.maxByOrNull { (_, result) ->
                val postView = result.getOrThrow()?.postView ?: return@maxByOrNull -1.0

                if (postView.post.featured_local || postView.post.featured_community) {
                    return@maxByOrNull Double.MAX_VALUE
                }

                when (sortType) {
                    SortType.Active ->
                        postView.counts.hot_rank_active
                            ?: postView.getActiveRank().also {
                                postView.counts.hot_rank_active = it
                            }

                    SortType.Hot ->
                        postView.counts.hot_rank
                            ?: postView.getHotRank().also {
                                postView.counts.hot_rank = it
                            }

                    SortType.New -> dateStringToTs(postView.counts.published).toDouble()
                    SortType.Old -> -dateStringToTs(postView.counts.published).toDouble()
                    SortType.MostComments -> postView.counts.comments.toDouble()
                    SortType.NewComments -> postView.counts.newest_comment_time.let {
                        if (it == null) {
                            0.0
                        } else {
                            dateStringToTs(it).toDouble()
                        }
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
                    SortType.TopNineMonths,
                    -> postView.counts.score.toDouble()
                    SortType.Controversial ->
                        postView.counts.controversy_rank
                            ?: postView.getControversialRank().also {
                                postView.counts.controversy_rank = it
                            }
                    SortType.Scaled -> {
                        val key = postView.community.toCommunityRef().fullName
                        var communityMau = communityToMau[key]

                        if (communityMau == null) {
                            apiClient
                                .fetchCommunityWithRetry(
                                    idOrName = Either.Left(postView.community.id),
                                    force = false,
                                )
                                .onSuccess {
                                    communityToMau[key] = it.community_view.counts.users_active_month
                                }
                            communityMau = communityToMau[key]
                        }

                        postView.counts.scaled_rank
                            ?: postView.getScaledRank(communityMau).also {
                                postView.counts.scaled_rank = it
                            }
                    }
                    null ->
                        postView.counts.score.toDouble()
                }
            }
            val nextItem = nextSourceAndResult?.second?.getOrNull()

            if (nextItem == null) {
                // no more items!
                hasMore = false
                break
            }

            Log.d(
                TAG,
                "Adding item ${nextItem.postView.post.id} from source ${nextItem::class.java}",
            )

            pageItems.add(nextItem)

            // increment the max item
            nextSourceAndResult.first.next()

            if (pageItems.size >= LemmyListSource.DEFAULT_PAGE_SIZE) {
                break
            }
        }

        return@a Result.success(
            Page(
                pageItems,
                pagesCache.size,
                instance,
                hasMore,
            ),
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
        communityNotFoundOnInstance.clear()
    }

    class EndReachedException : Exception()
    class CommunityNotFoundException(
        val instance: String,
        val communityRef: CommunityRef.CommunityRefByName,
    ) : Exception()
}
