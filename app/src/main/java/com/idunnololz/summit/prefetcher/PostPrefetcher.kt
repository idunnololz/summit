package com.idunnololz.summit.prefetcher

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.CommentsFetcher
import com.idunnololz.summit.api.dto.lemmy.CommentSortType
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.PostRef
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

  suspend fun prefetchPosts(
    postOrCommentRefs: List<Either<PostRef, CommentRef>>,
    sortOrder: CommentSortType,
    maxDepth: Int?,
    account: Account?,
    onProgress: suspend (count: Int, maxCount: Int) -> Unit,
  ) {
    var count = 0
    for (postOrCommentRef in postOrCommentRefs) {
      Log.d(TAG, "Prefetching post $postOrCommentRef")
      val apiClient = apiClientFactory.create()
      apiClient.changeInstance(
        postOrCommentRef.fold(
          { it.instance },
          { it.instance },
        ),
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
              account = account,
            )
          },
          {
            commentsFetcher.fetchCommentsWithRetry(
              id = Either.Right(it.id),
              sort = sortOrder,
              maxDepth = maxDepth,
              force = false,
              account = account,
            )
          },
        )

      count++

      onProgress(count, postOrCommentRefs.size)
    }
  }
}
