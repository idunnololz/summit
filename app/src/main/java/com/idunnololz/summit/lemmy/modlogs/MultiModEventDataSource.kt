package com.idunnololz.summit.lemmy.modlogs

import android.util.Log
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.ModlogActionType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class MultiModEventDataSource(
    private val instance: String,
    private val sources: List<LemmyListSource<ModEvent, Unit>>,
    private val pageSize: Int,
) {
    companion object {

        private const val TAG = "MultiModEventDataSource"

        fun create(
            apiClient: LemmyApiClient,
            communityIdOrNull: Int?,
            instance: String,
            limit: Int,
        ): MultiModEventDataSource {
            val types = listOf(
                ModlogActionType.ModRemovePost, //
                ModlogActionType.ModLockPost, //
                ModlogActionType.ModFeaturePost, //
                ModlogActionType.ModRemoveComment, //
                ModlogActionType.ModRemoveCommunity, //
                ModlogActionType.ModBanFromCommunity, //
                ModlogActionType.ModAddCommunity, //
                ModlogActionType.ModTransferCommunity, //
                ModlogActionType.ModAdd, //
                ModlogActionType.ModBan, //
                ModlogActionType.ModHideCommunity, //
                ModlogActionType.AdminPurgePerson, //
                ModlogActionType.AdminPurgeCommunity, //
                ModlogActionType.AdminPurgePost, //
                ModlogActionType.AdminPurgeComment, //
            )

            val sources = types.map { type ->
                LemmyListSource<ModEvent, Unit>(
                    { this.id },
                    Unit,
                    { page: Int, sortOrder: Unit, limit: Int, force: Boolean ->
                        apiClient.fetchModLogs(
                            personId = null,
                            communityId = communityIdOrNull,
                            page = page,
                            limit = limit,
                            actionType = type,
                            otherPersonId = null,
                            account = null,
                            force = force,
                        ).fold(
                            {
                                Result.success(it.toModEvents().sortedByDescending { it.ts })
                            },
                            {
                                Result.failure(it)
                            },
                        )
                    },
                    10,
                    type,
                )
            }

            return MultiModEventDataSource(instance, sources, limit)
        }
    }

    data class Page(
        val events: List<ModEvent>,
        val pageIndex: Int,
        val instance: String,
        val hasMore: Boolean,
    )

    private var pagesCache = mutableListOf<Page>()
    private var sortType: SortType? = null

    private val pagesContext = Dispatchers.Default.limitedParallelism(1)

    private val validSources
        get() = sources

    suspend fun fetchModEvents(
        page: Int,
        force: Boolean,
    ): Result<List<ModEvent>> = withContext(Dispatchers.Default) {
        if (force) {
            reset()
        }

        // prefetch if needed
        val prefetchJobs = validSources.map {
            async {
                it.peekNextItem()
            }
        }
        prefetchJobs.forEach {
            it.await()
        }

        fetchPage(
            page,
        ).fold(
            onSuccess = {
                Result.success(it.events)
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

    val sourcesCount: Int
        get() = sources.size

    private suspend fun fetchPage(
        pageIndex: Int,
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
        val pageItems = mutableListOf<ModEvent>()

        while (true) {
            val validSources = validSources
            var sourceToResult = validSources.map { it to it.peekNextItem() }
            val sourceAndError = sourceToResult
                .firstOrNull { (_, result) -> result.isFailure }

            if (sourceAndError != null) {
                val exception = requireNotNull(sourceAndError.second.exceptionOrNull())
                return@a Result.failure(exception)
            }
            sourceToResult = sourceToResult.filter { it.second.isSuccess }

            val nextSourceAndResult = sourceToResult.maxBy { (_, result) ->
                val modEvent = result.getOrThrow() ?: return@maxBy 0

                modEvent.ts
            }
            val nextItem = nextSourceAndResult.second.getOrNull()

            if (nextItem == null) {
                // no more items!
                hasMore = false
                break
            }

            Log.d(
                TAG,
                "Adding item ${nextItem.id} from source ${nextSourceAndResult.first.source}",
            )

            pageItems.add(nextItem)

            // increment the max item
            nextSourceAndResult.first.next()

            if (pageItems.size >= pageSize) {
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

    private fun reset() {
        sources.forEach {
            it.invalidate()
        }
        pagesCache.clear()
    }

    class EndReachedException : Exception()
}
