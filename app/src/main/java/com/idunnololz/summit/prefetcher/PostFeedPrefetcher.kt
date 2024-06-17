package com.idunnololz.summit.prefetcher

import android.util.Log
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.PostsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Singleton
class PostFeedPrefetcher @Inject constructor(
    coroutineScopeFactory: CoroutineScopeFactory,
) {
    companion object {
        private const val TAG = "PostFeedPrefetcher"
    }

    private val coroutineScope = coroutineScopeFactory.create()
    private var prefetchingJob: Job? = null

    fun prefetchPage(
        pageIndex: Int,
        postsRepository: PostsRepository,
        coroutineScope: CoroutineScope = this.coroutineScope,
    ) {
        Log.d(TAG, "Prefetching page $pageIndex")

        prefetchingJob = coroutineScope.launch(Dispatchers.IO) {
            suspendPrefetchPage(pageIndex = pageIndex, postsRepository = postsRepository)
        }
    }

    suspend fun suspendPrefetchPage(
        pageIndex: Int,
        postsRepository: PostsRepository,
    ): Result<PostsRepository.PageResult> {
        return postsRepository.getPage(pageIndex, dispatcher = Dispatchers.IO)
    }
}
