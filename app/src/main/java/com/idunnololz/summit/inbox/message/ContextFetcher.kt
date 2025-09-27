package com.idunnololz.summit.inbox.message

import android.app.Application
import androidx.core.content.ContextCompat
import arrow.core.Either
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.PendingCommentsManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.CommentsFetcher
import com.idunnololz.summit.api.dto.lemmy.CommentSortType
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.PostView
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.filterLists.ContentFiltersManager
import com.idunnololz.summit.lemmy.CommentNodeData
import com.idunnololz.summit.lemmy.CommentTreeBuilder
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.scopes.ViewModelScoped
import java.lang.AutoCloseable
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ViewModelScoped
class ContextFetcher @Inject constructor(
  private val application: Application,
  private val apiClient: AccountAwareLemmyClient,
  private val contentFiltersManager: ContentFiltersManager,
  private val accountActionsManager: AccountActionsManager,
  private val pendingCommentsManager: PendingCommentsManager,
  val accountManager: AccountManager,
  coroutineScopeFactory: CoroutineScopeFactory,
) : AutoCloseable {

  private val coroutineScope = coroutineScopeFactory.create()
  private val commentsFetcher = CommentsFetcher(apiClient)

  val commentContextFlow = MutableStateFlow<StatefulData<CommentContext>?>(null)

  private var currentMessageContext: CurrentMessageContext? = null

  init {
    coroutineScope.launch(Dispatchers.Default) {
      accountActionsManager.onCommentActionChanged.collect {
        val currentMessageContext = currentMessageContext
        if (currentMessageContext != null) {
          if (pendingCommentsManager
              .getPendingComments(currentMessageContext.postRef).isNotEmpty()
          ) {
            withContext(Dispatchers.Main) {
              fetchCommentContext(
                currentMessageContext.postRef.id,
                currentMessageContext.commentPath,
                force = true,
              )
            }
          }
        }
      }
    }
  }

  suspend fun fetchCommentContext(
    postId: Int,
    commentPath: String?,
    force: Boolean,
  ): Result<CommentContext> {
    currentMessageContext = CurrentMessageContext(PostRef(apiClient.instance, postId), commentPath)

    commentContextFlow.emit(StatefulData.Loading())

    return _fetchCommentContext(
      postId = postId,
      commentPath = commentPath,
      force = force,
    ).also {
      it
        .onSuccess {
          commentContextFlow.emit(StatefulData.Success(it))
        }
        .onFailure {
          commentContextFlow.emit(StatefulData.Error(it))
        }
    }
  }

  private suspend fun _fetchCommentContext(
    postId: Int,
    commentPath: String?,
    force: Boolean,
  ): Result<CommentContext> = withContext(Dispatchers.Default) {
    val postJob = async {
      apiClient.fetchPostWithRetry(Either.Left(postId), force)
    }
    val commentJob = if (commentPath != null) {
      async {
        fetchCompleteCommentPath(commentPath = commentPath, force = force)
      }
    } else {
      null
    }

    val postResult = postJob.await()
    val commentResult = commentJob?.await()

    if (postResult.isFailure) {
      return@withContext Result.failure(requireNotNull(postResult.exceptionOrNull()))
    }

    if (commentResult?.isFailure == true) {
      return@withContext Result.failure(requireNotNull(commentResult.exceptionOrNull()))
    }

    val tree = CommentTreeBuilder(
      context = ContextCompat.getContextForLanguage(application),
      accountManager = accountManager,
      contentFiltersManager = contentFiltersManager,
    ).buildCommentsTreeListView(
      post = null,
      comments = commentResult?.getOrNull(),
      pendingComments = null,
      supplementaryComments = mapOf(),
      removedCommentIds = setOf(),
      fullyLoadedCommentIds = setOf(),
      targetCommentRef = null,
      singleCommentChain = null,
    )

    Result.success(
      CommentContext(
        requireNotNull(postResult.getOrNull()?.post_view),
        tree.firstOrNull(),
      ),
    )
  }

  private suspend fun fetchCompleteCommentPath(
    commentPath: String,
    force: Boolean,
  ): Result<List<CommentView>> {
    val commentIds = commentPath.split(".").map { it.toInt() }
    val topCommentId = commentIds.firstOrNull { it != 0 }
    val result = mutableListOf<CommentView>()

    if (topCommentId == null) {
      return Result.failure(RuntimeException("No context found."))
    }

    return commentsFetcher
      .fetchAllCommentsWithRetry(
        id = Either.Right(topCommentId),
        sort = null,
        maxDepth = null,
        force = force,
      )
      .fold(
        {
          result.addAll(it)

          val furthestCommentSeen = it.maxByOrNull { commentIds.indexOf(it.comment.id) }

          if (furthestCommentSeen == null) {
            Result.failure(RuntimeException("Can't fetch comment."))
          } else {
            Result.success(result)
          }
        },
        {
          Result.failure(it)
        }
      )
  }

  override fun close() {
    coroutineScope.cancel()
  }

  data class CommentContext(
    val post: PostView,
    val commentTree: CommentNodeData?,
  )

  data class CurrentMessageContext(
    val postRef: PostRef,
    val commentPath: String?,
  )
}
