package com.idunnololz.summit.lemmy.mod.viewVotes

import android.content.Context
import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.VoteView
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource.PageResult
import com.idunnololz.summit.lemmy.inbox.repository.MultiLemmyListSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

class VotesRepository @AssistedInject constructor(
  @ApplicationContext private val context: Context,
  private val apiClient: AccountAwareLemmyClient,
  @Assisted private val postOrCommentId: Either<PostId, CommentId>,
) {

  companion object {
    private const val TAG = "VotesRepository"
  }

  @ViewModelScoped
  @AssistedFactory
  interface Factory {
    fun create(postOrCommentId: Either<PostId, CommentId>): VotesRepository
  }

  private val dataSource =
    MultiLemmyListSource(
      listOf(
        LemmyListSource<VoteView, Unit>(
          context,
          { this.creator.id },
          defaultSortOrder = Unit,
          { pageIndex: Int,
            sortOrder: Unit,
            limit: Int,
            force: Boolean,
            ->

            postOrCommentId.fold(
              {
                apiClient.listPostVotesWithRetry(
                  postId = it,
                  page = pageIndex.toLong(),
                  limit = limit.toLong(),
                  force = force,
                ).map { it.post_likes }
              },
              {
                apiClient.listCommentVotesWithRetry(
                  commentId = it,
                  page = pageIndex.toLong(),
                  limit = limit.toLong(),
                  force = force,
                ).map { it.comment_likes }
              },
            )
          },
        ),
      ),
      sortValue = { it.score },
      id = { it.creator.id },
    )

  suspend fun getPage(
    pageIndex: Int,
    force: Boolean,
    retainItemsOnForce: Boolean = false,
  ): Result<PageResult<VoteView>> {
    val result = dataSource.getPage(pageIndex, force, retainItemsOnForce)

    Log.d(TAG, "Got ${result.getOrNull()?.items?.size} items")

    return result
  }

  fun invalidate() {
    dataSource.invalidate()
  }
}
