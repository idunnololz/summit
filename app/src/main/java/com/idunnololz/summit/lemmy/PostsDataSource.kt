package com.idunnololz.summit.lemmy

import android.content.Context
import androidx.work.WorkerParameters
import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.actions.PendingActionsManager
import com.idunnololz.summit.actions.PendingActionsRunner
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import com.idunnololz.summit.lemmy.actions.LemmyActionResult
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource.Companion.PAGE_SIZE
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import java.util.LinkedList
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

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
        fun create(
            communityName: String?,
            listingType: ListingType?,
        ): SinglePostsDataSource
    }

    override suspend fun fetchPosts(
        sortType: SortType?,
        page: Int,
        force: Boolean,
    ) = apiClient.fetchPosts(
        if (communityName == null) {
            null
        } else {
            Either.Right(communityName)
        },
        sortType,
        listingType ?: ListingType.All,
        page.toLemmyPageIndex(),
        PAGE_SIZE,
        force,
    )

    private fun Int.toLemmyPageIndex() =
        this + 1 // lemmy pages are 1 indexed
}