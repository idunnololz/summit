package com.idunnololz.summit.prefetcher

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.CommentsFetcher
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.PostRef
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostPrefetcher @Inject constructor(
    coroutineScopeFactory: CoroutineScopeFactory,
    private val apiClientFactory: AccountAwareLemmyClient.Factory,
) {
    companion object {
        private const val TAG = "PostPrefetcher"
    }

    private val coroutineScope = coroutineScopeFactory.create()
    private var prefetchingJob: Job? = null

    fun prefetchPosts(
        postOrCommentRefs: List<Either<PostRef, CommentRef>>,
        sortOrder: CommentSortType,
        maxDepth: Int?,
    ) {
        prefetchingJob?.cancel()

        prefetchingJob = coroutineScope.launch {
            for (postOrCommentRef in postOrCommentRefs) {
                Log.d(TAG, "Prefetching post $postOrCommentRef")
                val apiClient = apiClientFactory.create()
                apiClient.changeInstance(
                    postOrCommentRef.fold(
                        { it.instance },
                        { it.instance },
                    )
                )
                val commentsFetcher = CommentsFetcher(apiClient)

                postOrCommentRef
                    .fold(
                        {
                            commentsFetcher.fetchCommentsWithRetry(
                                id = Either.Left(it.id),
                                sort = sortOrder,
                                maxDepth = maxDepth,
                                force = false,
                            )
                        },
                        {
                            commentsFetcher.fetchCommentsWithRetry(
                                id = Either.Right(it.id),
                                sort = sortOrder,
                                maxDepth = maxDepth,
                                force = false,
                            )
                        },
                    )
            }
        }
    }
}