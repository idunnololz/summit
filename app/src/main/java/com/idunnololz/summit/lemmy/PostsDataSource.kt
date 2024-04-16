package com.idunnololz.summit.lemmy

import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource.Companion.DEFAULT_PAGE_SIZE
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

interface PostsDataSource {
    suspend fun fetchPosts(
        sortType: SortType? = null,
        page: Int,
        force: Boolean,
    ): Result<List<PostView>>
}

class SinglePostsDataSource @AssistedInject constructor(
    @Assisted private val communityName: String?,
    @Assisted private val listingType: ListingType?,
    private val apiClient: AccountAwareLemmyClient,
) : PostsDataSource {

    @AssistedFactory
    interface Factory {
        fun create(communityName: String?, listingType: ListingType?): SinglePostsDataSource
    }

    override suspend fun fetchPosts(sortType: SortType?, page: Int, force: Boolean) =
        apiClient.fetchPosts(
            if (communityName == null) {
                null
            } else {
                Either.Right(communityName)
            },
            sortType,
            listingType ?: ListingType.All,
            page.toLemmyPageIndex(),
            DEFAULT_PAGE_SIZE,
            force,
        )
}
