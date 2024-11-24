package com.idunnololz.summit.lemmy

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.GetPostsResponse
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource.Companion.DEFAULT_PAGE_SIZE
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.lemmy.multicommunity.Source
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

interface PostDataSourceWithCursor {
    suspend fun fetchNextPage(
        sortType: SortType? = null,
        force: Boolean,
    ): Result<PageWithCursorResult>

    suspend fun reset()

    val nextCursor: String
}

data class PageWithCursorResult(
    val posts: List<FetchedPost>,
    val cursor: String,
    val hasMore: Boolean,
)

class SinglePostDataSourceWithCursor @AssistedInject constructor(
    @Assisted
    private val fetchPosts: suspend (cursor: String?, force: Boolean) -> Result<GetPostsResponse>,
    private val apiClient: AccountAwareLemmyClient,
) : PostDataSourceWithCursor {

    @AssistedFactory
    interface Factory {
        fun create(
            fetchPosts: suspend (cursor: String?, force: Boolean) -> Result<GetPostsResponse>
        ): SinglePostDataSourceWithCursor
    }

    private var cursor: String? = null

    override val nextCursor: String
        get() = cursor ?: "first"

    override suspend fun fetchNextPage(
        sortType: SortType?,
        force: Boolean,
    ): Result<PageWithCursorResult> {
        val curCursor = nextCursor

        return fetchPosts(cursor, force)
                .map {
                    cursor = it.next_page

                    Log.d("VotedViewModel", "it.next_page ${it.next_page}")

                    PageWithCursorResult(
                        posts = it.posts.map { FetchedPost(it, Source.StandardSource()) },
                        cursor = curCursor,
                        hasMore = curCursor != it.next_page,
                    )
                }
    }

    override suspend fun reset() {
        cursor = null
    }
}
