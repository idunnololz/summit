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
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import javax.inject.Inject

class MultiCommunityDataSource(
    private val sources: List<LemmyListSource<PostView, SortType>>
) {
    class Factory @Inject constructor(
        private val apiClient: AccountAwareLemmyClient,
    ) {
        fun create(
            communities: List<CommunityRef.CommunityRefByName>
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

            return MultiCommunityDataSource(sources)
        }
    }

    /**
     * @return true if there might be more posts to fetch
     */
    private suspend fun fetchPage(
        pageIndex: Int,
        sortType: SortType,
        listingType: ListingType,
        force: Boolean,
    ): Result<Boolean> {
        for (source in sources) {
            source.sortOrder
        }
        val newPosts =
            apiClient.fetchPosts(
                communityIdOrName = communityIdOrName,
                sortType = sortType,
                listingType = listingType,
                page = pageIndex,
                limit = 20,
                force = force,
            )
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

                Log.d(PostsRepository.TAG, "Fetched ${newPosts.size} posts.")
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
}