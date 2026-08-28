package com.idunnololz.summit.api

import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.api.dto.lemmy.CommentSortType
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.PostId
import com.idunnololz.summit.util.arrow.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommentsFetcher(
  private val apiClient: AccountAwareLemmyClient,
) {

  suspend fun fetchCommentsWithRetry(
    id: Either<PostId, CommentId>,
    sort: CommentSortType?,
    maxDepth: Int? = null,
    limit: Int? = null,
    page: Int? = null,
    force: Boolean = false,
    account: Account? = apiClient.accountForInstance(),
  ): Result<List<CommentView>> = withContext(Dispatchers.IO) {
    apiClient.fetchCommentsWithRetry(
      id = id,
      sort = sort,
      maxDepth = maxDepth,
      limit = limit,
      page = page,
      force = force,
      account = account,
    )
  }

  suspend fun fetchAllCommentsWithRetry(
    id: Either<PostId, CommentId>,
    sort: CommentSortType?,
    maxDepth: Int?,
    force: Boolean = false,
    account: Account? = apiClient.accountForInstance(),
  ): Result<List<CommentView>> = withContext(Dispatchers.IO) {
    var page = 0
    val comments = mutableListOf<CommentView>()
    val limit = 20

    while (true) {
      val result = fetchCommentsWithRetry(
        id = id,
        sort = sort,
        maxDepth = maxDepth,
        // setting a limit doesn't seem to do anything...
        limit = limit,
        page = page,
        force = force,
        account = account,
      )

      result
        .onSuccess {
          comments.addAll(it)

          if (it.size < limit) {
            break
          }
        }
        .onFailure {
          return@withContext result
        }

      page++
    }

    Result.success(comments)
  }
}
