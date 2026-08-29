package com.idunnololz.summit.lemmy.post

import android.app.Application
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.actions.PendingCommentView
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.CommentsFetcher
import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.api.dto.lemmy.CommentSortType
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.filterLists.ContentFiltersManager
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommentTreeBuilder
import com.idunnololz.summit.lemmy.CommentsSortOrder
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.duplicatePostsDetector.DuplicatePostsDetector
import com.idunnololz.summit.lemmy.post.PostViewModel.Companion.HIGHLIGHT_COMMENT_MS
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.toPostHeaderInfo
import com.idunnololz.summit.lemmy.utils.toVotableRef
import com.idunnololz.summit.models.GetPostResponse
import com.idunnololz.summit.models.PostView
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.arrow.Either
import com.idunnololz.summit.util.dateStringToTs
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PostAndCommentsLoader @AssistedInject constructor(
  private val accountActionsManager: AccountActionsManager,
  private val accountManager: AccountManager,
  private val contentFiltersManager: ContentFiltersManager,
  private val postReadManager: PostReadManager,
  private val duplicatePostsDetector: DuplicatePostsDetector,
  @Assisted private val context: Application,
  @Assisted private val coroutineScope: CoroutineScope,
  @Assisted var initialMaxDepth: Int,
  @Assisted defaultSortOrder: CommentsSortOrder,
  @Assisted private val postRef: PostRef,
  @Assisted private val commentRef: CommentRef?,
  @Assisted initialPostView: PostView?,
  @Assisted private val preferUserDisplayName: Boolean,
  @Assisted private val lemmyApiClient: AccountAwareLemmyClient,
  @Assisted var getPostResponse: GetPostResponse?,
  @Assisted private val currentAccountView: MutableLiveData<AccountView?>,
  @Assisted private val callback: Callback,
) {

  @AssistedFactory
  interface Factory {
    fun create(
      context: Application,
      coroutineScope: CoroutineScope,
      initialMaxDepth: Int,
      defaultSortOrder: CommentsSortOrder,
      postRef: PostRef,
      commentRef: CommentRef?,
      initialPostView: PostView?,
      preferUserDisplayName: Boolean,
      lemmyApiClient: AccountAwareLemmyClient,
      getPostResponse: GetPostResponse?,
      currentAccountView: MutableLiveData<AccountView?>,
      callback: Callback,
    ): PostAndCommentsLoader
  }

  interface Callback {
    fun onPostViewLoaded(postView: PostView)
    fun onPostModelChanged(postModel: StatefulData<PostModel>)
  }

  companion object {
    private const val TAG = "PostAndCommentsLoader"
  }

  val postModel = MutableStateFlow<StatefulData<PostModel>>(StatefulData.NotStarted())

  var selectedCommentId: Int? = null

  val mutex = Mutex()

  var commentsSortOrder = defaultSortOrder

  val apiInstance: String
    get() = lemmyApiClient.instance

  val postView: PostView?
    get() = postViewFlow.value?.getOrNull()

  private var pendingComments: List<PendingCommentView>? = null
  private val commentsFetcher = CommentsFetcher(lemmyApiClient)
  private val commentsFlow = MutableStateFlow<Result<List<CommentView>>?>(null)
  private val postViewFlow = MutableStateFlow<Result<PostView>?>(
    initialPostView?.let {
      Result.success(initialPostView)
    }
  )
  private var newlyPostedCommentId: CommentId? = null
  private val additionalLoadedCommentIds = mutableSetOf<CommentId>()
  private val removedCommentIds = mutableSetOf<CommentId>()

  /**
   * Comments that didn't load by default but were loaded by the user requesting additional comments
   */
  private var supplementaryComments = mutableMapOf<Int, CommentView>()

  /**
   * This is used for the edge case where a comment is fully loaded and some of it's direct
   * descendants are missing. This can be used to check if comments are missing or just not
   * loaded yet.
   */
  private val fullyLoadedCommentIds = mutableSetOf<CommentId>()

  init {
    coroutineScope.launch {
      postModel.collect {
        callback.onPostModelChanged(it)
      }
    }
  }

  fun updatePendingComments(
    resolveCompletedPendingComments: Boolean,
  ) {
    val sortOrder = commentsSortOrder.toApiSortOrder()

    coroutineScope.launch {
      mutex.withLock {
        updatePendingCommentsInternalLocked(
          sortOrder = sortOrder,
          resolveCompletedPendingComments = resolveCompletedPendingComments,
        )
      }

      updateData(wasUpdateForced = false)
    }
  }

  private suspend fun updatePendingCommentsInternalLocked(
    sortOrder: CommentSortType,
    resolveCompletedPendingComments: Boolean,
  ) {
    Log.d(TAG, "updatePendingCommentsInternalLocked()")
    val postRef = postRef

    pendingComments = accountActionsManager.getPendingComments(postRef)

    var modified = false
    if (resolveCompletedPendingComments) {
      val completedPendingComments = pendingComments?.filter { it.complete } ?: listOf()
      val anyPendingCommentComplete = completedPendingComments.isNotEmpty()

      if (anyPendingCommentComplete) {
        var result: Result<List<CommentView>>? = null

        for (i in 0 until 10) {
          // Looks like commits on the server is async. Refreshing a comment immediately
          // after we post it may not get us the latest value.

          Log.d(TAG, "updatePendingCommentsInternalLocked() - checking for updated comment")

          result = commentsFetcher.fetchCommentsWithRetry(
            id = Either.Left(postRef.id),
            sort = sortOrder,
            // We use [initialMaxDepth] here to prevent expanding all comments if
            // [collapseChildCommentsByDefault] is on.
            maxDepth = initialMaxDepth,
            force = true,
          )

          val oldComments = commentsFlow.value?.getOrNull()
          val newComments = result.getOrNull()

          var allCommentsUpdated = true // tracks if all comments are updated on the server

          if (oldComments != null && newComments != null) {
            for (completedPendingComment in completedPendingComments) {
              val commentId = completedPendingComment.commentId
              if (commentId != null) {
                val oldComment = oldComments
                  .firstOrNull { it.comment.id == commentId }
                  ?: continue
                val newComment = newComments
                  .firstOrNull { it.comment.id == commentId }
                  ?: continue

                if (oldComment.comment.updated == newComment.comment.updated) {
                  Log.d(
                    TAG,
                    "updatePendingCommentsInternalLocked() - 1 completed pending comment was not " +
                      "updated on the server.",
                  )
                  allCommentsUpdated = false
                } else {
                  Log.d(
                    TAG,
                    "updatePendingCommentsInternalLocked() - 1 completed pending comment was " +
                      "updated on the server. New content: '${newComment.comment.content}'",
                  )
                }
              }
            }
          }

          // Set the comments we fetched as supplementary comments since the comments we fetch
          // might be considered incomplete.
          newComments?.forEach {
            supplementaryComments[it.comment.id] = it
          }

          if (allCommentsUpdated) {
            delay(600)
            break
          }
        }

        Log.d(TAG, "updatePendingCommentsInternalLocked() - comments up-to-date")

        requireNotNull(result)

        completedPendingComments.forEach { pendingComment ->
          val commentsResult = if (pendingComment.parentId == null) {
            result
          } else {
            fetchMoreCommentsInternalLocked(
              parentId = pendingComment.parentId,
              sortOrder = sortOrder,
              force = true,
            )
          }

          commentsResult.onSuccess {
            modified = true

            // find the comment that was recently posted by guessing!

            if (pendingComment.isActionDelete) {
              newlyPostedCommentId = pendingComment.commentId
            } else if (pendingComment.commentId != null) {
              newlyPostedCommentId = pendingComment.commentId
            } else {
              newlyPostedCommentId = it
                .sortedByDescending { commentView ->
                  dateStringToTs(
                    commentView.comment.updated
                      ?: commentView.comment.published,
                  )
                }
                .firstOrNull {
                  it.comment.creator_id ==
                    accountManager.currentAccount.asAccount?.id
                }
                ?.comment?.id
            }

            accountActionsManager.removePendingComment(pendingComment)
          }
        }

        withContext(Dispatchers.Main) {
          fetchPostData(
            fetchPostData = true,
            fetchCommentData = false,
            force = true,
          )
        }
      }
    }

    if (modified) {
      pendingComments = accountActionsManager.getPendingComments(postRef)
    }
  }

  private suspend fun updateData(wasUpdateForced: Boolean) = withContext(Dispatchers.Default) {
    Log.d(
      TAG,
      "updateData() - pendingComments: ${pendingComments?.size ?: 0} comments: ${commentsFlow.value}",
    )

    val context = ContextCompat.getContextForLanguage(context)
    val postResult = postViewFlow.value ?: return@withContext
    val post = postResult.getOrNull()
    val postError = postResult.exceptionOrNull()
    val commentsResult = commentsFlow.value
    val comments = commentsResult?.getOrNull()
    val crossPosts = getPostResponse?.crossPosts
    val pendingComments = pendingComments
    val supplementaryComments = supplementaryComments
    val isSingleCommentChain = commentRef != null

    val postModelValue = PostModel(
      postListView = if (post != null) {
        PostListItem.PostLoadedListView(
          post = post,
          postHeaderInfo = post.toPostHeaderInfo(context),
        )
      } else {
        PostListItem.PostErrorListView(requireNotNull(postError))
      },
      commentTree = CommentTreeBuilder(
        context = context,
        accountManager = accountManager,
        contentFiltersManager = contentFiltersManager,
        preferUserDisplayName = preferUserDisplayName,
      ).buildCommentsTreeListView(
        post = post,
        comments = comments,
        pendingComments = pendingComments,
        supplementaryComments = supplementaryComments,
        removedCommentIds = removedCommentIds,
        fullyLoadedCommentIds = fullyLoadedCommentIds,
        targetCommentRef = commentRef,
        singleCommentChain = commentRef,
      ),
      crossPosts = crossPosts ?: listOf(),
      newlyPostedCommentId = newlyPostedCommentId,
      selectedCommentId = selectedCommentId
        ?: commentRef?.id,
      isSingleCommentChain = isSingleCommentChain,
      isNativePost = isNativePost(),
      accountInstance = currentAccountView.value?.account?.instance,
      isCommentsLoaded = comments != null,
      commentPath = comments?.firstOrNull()?.comment?.path,
      wasUpdateForced = wasUpdateForced,
      loadCommentError = commentsResult?.exceptionOrNull(),
    )

    postModel.value = StatefulData.Success(postModelValue)
  }

  fun isNativePost(): Boolean {
    val currentAccount = currentAccountView.value ?: return true

    val instance = postRef.instance
    return instance.equals(currentAccount.account.instance, ignoreCase = true)
  }

  private suspend fun fetchMoreCommentsInternalLocked(
    parentId: CommentId,
    sortOrder: CommentSortType,
    maxDepth: Int? = null,
    force: Boolean = false,
  ): Result<List<CommentView>> {
    Log.d(TAG, "fetchMoreCommentsInternal(): parentId = $parentId")
    val result = commentsFetcher.fetchCommentsWithRetry(
      id = Either.Right(parentId),
      sort = sortOrder,
      maxDepth = maxDepth,
      force = force,
    )

    additionalLoadedCommentIds.add(parentId)

    result
      .onSuccess { comments ->
        // A comment is likely removed if we are loading a specific comment and it's direct
        // descendant is missing

        val thisComment = comments.find { it.comment.id == parentId }

        if (comments.isEmpty() || thisComment == null) {
          removedCommentIds.add(parentId)
        } else {
          removedCommentIds.remove(parentId)
        }

        val depthIsMoreThanOne = maxDepth == null || maxDepth > 1
        if (thisComment != null && depthIsMoreThanOne) {
          fullyLoadedCommentIds.add(thisComment.comment.id)
        }

        comments.forEach {
          supplementaryComments[it.comment.id] = it
        }
      }
      .onFailure {
        if (it is ClientApiException && it.errorCode == 404) {
          // comment has been removed...
          removedCommentIds.add(parentId)
        }
      }

    return result
  }

  fun fetchPostData(
    fetchPostData: Boolean = true,
    fetchCommentData: Boolean = true,
    force: Boolean = false,
    markPostAsRead: Boolean = true,
  ): Job {
    Log.d(
      TAG,
      "fetchPostData(): fetchPostData = $fetchPostData " +
        "fetchCommentData = $fetchCommentData force = $force",
    )

    postModel.value = StatefulData.Loading()

    return coroutineScope.launch(Dispatchers.Default) {
      _fetchPostData(
        fetchPostData = fetchPostData,
        fetchCommentData = fetchCommentData,
        force = force,
        markPostAsRead = markPostAsRead,
      )
    }
  }

  fun fetchMoreComments(parentId: CommentId?, maxDepth: Int? = null, force: Boolean = false) {
    val sortOrder = commentsSortOrder.toApiSortOrder()

    coroutineScope.launch {
      if (parentId != null) {
        fetchMoreCommentsInternal(
          parentId = parentId,
          sortOrder = sortOrder,
          maxDepth = maxDepth,
          force = force,
        )
      } else {
        // TODO maybe?
      }

      updateData(wasUpdateForced = force)
    }
  }

  fun resetNewlyPostedComment() {
    newlyPostedCommentId = null

    coroutineScope.launch {
      delay(HIGHLIGHT_COMMENT_MS)

      updateData(wasUpdateForced = false)
    }
  }

  private suspend fun fetchMoreCommentsInternal(
    parentId: CommentId,
    sortOrder: CommentSortType,
    maxDepth: Int? = null,
    force: Boolean = false,
  ): Result<List<CommentView>> = mutex.withLock {
    fetchMoreCommentsInternalLocked(
      parentId = parentId,
      sortOrder = sortOrder,
      maxDepth = maxDepth,
      force = force,
    )
  }

  private suspend fun _fetchPostData(
    fetchPostData: Boolean = true,
    fetchCommentData: Boolean = true,
    force: Boolean = false,
    markPostAsRead: Boolean = true,
  ) {
    Log.d(
      TAG,
      "_fetchPostData(): fetchPostData = $fetchPostData fetchCommentData = $fetchCommentData",
    )
    mutex.withLock {
      val sortOrder = commentsSortOrder.toApiSortOrder()

      val postResult: Result<GetPostResponse?> =
        if (!markPostAsRead) {
          Result.failure(RuntimeException("Can't fetch post or else it will be marked as read!"))
        } else if (fetchPostData) {
          if (force || getPostResponse == null) {
            withContext(Dispatchers.IO) {
              lemmyApiClient.fetchPostWithRetry(Either.Left(postRef.id), force)
            }
          } else {
            Result.success(getPostResponse)
          }
        } else {
          Result.success(getPostResponse)
        }

      getPostResponse = if (force) {
        postResult.getOrNull()
      } else {
        postResult.getOrNull()
          ?: getPostResponse
      }
      val loadedPostView = getPostResponse?.postView

      if (loadedPostView != null) {
        postViewFlow.value = Result.success(loadedPostView)
      } else if (postViewFlow.value?.isSuccess == true) {
        // do nothing
      } else if (postResult.isFailure) {
        postViewFlow.value = Result.failure(requireNotNull(postResult.exceptionOrNull()))
      }

      loadedPostView?.let {
        callback.onPostViewLoaded(it)
      }

      updateData(wasUpdateForced = false)

      val commentsResult = if (fetchCommentData) {
        commentsFetcher.fetchCommentsWithRetry(
          id = Either.Left(postRef.id),
          sort = sortOrder,
          maxDepth = initialMaxDepth,
          force = force,
        )
      } else {
        commentsFlow.value
      }
      val newComments = commentsResult?.getOrNull()

      if (newComments != null) {
        commentsFlow.value = Result.success(newComments)
        invalidateSupplementaryComments(newComments)
      }

      if (force) {
        additionalLoadedCommentIds.forEach {
          fetchMoreCommentsInternalLocked(
            parentId = it,
            sortOrder = sortOrder,
            maxDepth = null,
            force = force,
          )
        }
      }

      updatePendingCommentsInternalLocked(sortOrder, true)

      val post = postResult.getOrNull()
      val comments = commentsResult?.getOrNull()
      val postView = post?.postView ?: postViewFlow.value?.getOrNull()

      if (postView != null) {
        if (markPostAsRead) {
          markPostAsRead(postView)
        }
        if (force) {
          accountActionsManager.setScore(postView.toVotableRef(), postView.counts.score)
        }
      }

      if (force) {
        if (comments != null && fetchCommentData) {
          comments.forEach {
            accountActionsManager.setScore(it.toVotableRef(), it.counts.score)
          }
        }
        if (postView != null && fetchPostData) {
          accountActionsManager.setScore(postView.toVotableRef(), postView.counts.score)
        }
      }

      updateData(wasUpdateForced = force)
    }
  }

  private fun invalidateSupplementaryComments(newComments: List<CommentView>) {
    for (comment in newComments) {
      val supComment = supplementaryComments[comment.comment.id] ?: continue
      val supTs = dateStringToTs(supComment.comment.updated ?: supComment.comment.published)
      val newTs = dateStringToTs(comment.comment.updated ?: comment.comment.published)
      if (newTs > supTs) {
        supplementaryComments.remove(comment.comment.id)
      }
    }
  }

  private suspend fun markPostAsRead(postView: PostView) {
    val postView = postView
    postReadManager.markPostAsReadLocal(apiInstance, postView.post.id, read = true)
    duplicatePostsDetector.addReadOrHiddenPost(postView)
  }
}