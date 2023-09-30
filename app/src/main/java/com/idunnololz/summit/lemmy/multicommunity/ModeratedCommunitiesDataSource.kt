package com.idunnololz.summit.lemmy.multicommunity

import arrow.core.Either
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.PostsDataSource
import com.idunnololz.summit.lemmy.toCommunityRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import javax.inject.Inject

class ModeratedCommunitiesDataSource(
    private val apiClient: AccountAwareLemmyClient,
    private val accountInfoManager: AccountInfoManager,
    private val dataSourceFactory: MultiCommunityDataSource.Factory,
) : PostsDataSource {

    companion object {
        private const val TAG = "ModeratedCommunitiesDataSource"

        private const val DEFAULT_PAGE_SIZE = 10

        /**
         * Maximum number of communities that can be added into one multi-community.
         */
        const val MULTI_COMMUNITY_DATA_SOURCE_LIMIT = 30
    }

    class Factory @Inject constructor(
        private val apiClient: AccountAwareLemmyClient,
        private val accountInfoManager: AccountInfoManager,
        private val dataSourceFactory: MultiCommunityDataSource.Factory,
    ) {
        fun create(): ModeratedCommunitiesDataSource {
            return ModeratedCommunitiesDataSource(apiClient, accountInfoManager, dataSourceFactory)
        }
    }

    private var dataSource: MultiCommunityDataSource? = null

    override suspend fun fetchPosts(
        sortType: SortType?,
        page: Int,
        force: Boolean,
    ): Result<List<PostView>> =
        getDataSource().fold(
            onSuccess = {
                if (it.sourcesCount == 0) {
                    Result.failure(NoModeratedCommunitiesException())
                } else {
                    it.fetchPosts(sortType, page, force)
                }
            },
            onFailure = {
                Result.failure(it)
            },
        )

    private suspend fun getDataSource(): Result<MultiCommunityDataSource> {
        val dataSource = dataSource
        if (dataSource != null) {
            return Result.success(dataSource)
        }

        val fullAccount = accountInfoManager
            .currentFullAccount
            .value ?: return Result.failure(NotAuthenticatedException())

        val moderatedCommunityIds = fullAccount
            .accountInfo
            .miscAccountInfo
            ?.modCommunityIds
            ?: listOf()

        val communityRefs = flow {
            moderatedCommunityIds.forEach { community ->
                apiClient
                    .fetchCommunityWithRetry(Either.Left(community), false)
                    .onSuccess {
                        emit(it.community_view.community.toCommunityRef())
                    }
            }
        }.flowOn(Dispatchers.Default).toList()

        return dataSourceFactory.create(fullAccount.account.instance, communityRefs).let {
            this.dataSource = it
            Result.success(it)
        }
    }
}

class NoModeratedCommunitiesException : Exception()
