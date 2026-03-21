package com.idunnololz.summit.lemmy.post

import android.app.Application
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.actions.PendingCommentView
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.COMMENTS_DEPTH_MAX
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.CommentsFetcher
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.api.dto.lemmy.CommentSortType
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.models.PostView
import com.idunnololz.summit.filterLists.ContentFiltersManager
import com.idunnololz.summit.lemmy.CommentNavControlsState
import com.idunnololz.summit.lemmy.CommentNodeData
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommentTreeBuilder
import com.idunnololz.summit.lemmy.CommentsSortOrder
import com.idunnololz.summit.lemmy.Consts.DEFAULT_INSTANCE
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.duplicatePostsDetector.DuplicatePostsDetector
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toPostHeaderInfo
import com.idunnololz.summit.lemmy.utils.toVotableRef
import com.idunnololz.summit.localTracking.LocalTracker
import com.idunnololz.summit.localTracking.TrackedAction
import com.idunnololz.summit.models.GetPostResponse
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.dateStringToTs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
@HiltViewModel
class PostViewModel @Inject constructor(
  private val context: Application,
  private val lemmyApiClientFactory: AccountAwareLemmyClient.Factory,
  private val accountActionsManager: AccountActionsManager,
  private val accountManager: AccountManager,
  private val accountInfoManager: AccountInfoManager,
  private val postReadManager: PostReadManager,
  private val preferenceManager: PreferenceManager,
  private val state: SavedStateHandle,
  private val unauthedApiClient: LemmyApiClient,
  private val contentFiltersManager: ContentFiltersManager,
  private val duplicatePostsDetector: DuplicatePostsDetector,
  val queryMatchHelper: QueryMatchHelper,
  private val tracker: LocalTracker,
) : ViewModel() {

  companion object {
    private const val TAG = "PostViewModel"

    const val HIGHLIGHT_COMMENT_MS = 3_500L
  }

  /**
   * Create a new instance so we can change the instance without screwing up app state
   */
  val lemmyApiClient = lemmyApiClientFactory.create()

  val selectedCommentId = state.getMutableStateFlow<Int?>("selected_comment_id", null)
  var postOrCommentRef: Either<PostRef, CommentRef>? = null
    set(value) {
      field = value

      state["postRef"] = value?.leftOrNull()
      state["commentRef"] = value?.getOrNull()
    }
  var postRef: PostRef? = state.get<PostRef>("postRef2")
    set(value) {
      field = value

      state["postRef2"] = value
    }
  var currentAccountIdOverride: Long? = null
  val onPostOrCommentRefChange = MutableLiveData<Either<PostRef, CommentRef>>()

  val currentAccountView = MutableLiveData<AccountView?>()

  val findInPageVisible = MutableLiveData<Boolean>(false)
  val findInPageQuery = MutableLiveData<String>("")
  val screenshotMode = MutableLiveData<Boolean>(false)
  private val findInPageQueryFlow = MutableStateFlow<String>("")

  private var getPostResponse: GetPostResponse? = null
  private val postViewFlow = MutableStateFlow<Result<PostView>?>(null)
  private val commentsFlow = MutableStateFlow<Result<List<CommentView>>?>(null)
  private var pendingComments: List<PendingCommentView>? = null
  private var newlyPostedCommentId: CommentId? = null

  private val additionalLoadedCommentIds = mutableSetOf<CommentId>()
  private val removedCommentIds = mutableSetOf<CommentId>()
  private val visitTracked = state.getMutableStateFlow("visitTracked", false)

  private val mutex = Mutex()

  /**
   * This is used for the edge case where a comment is fully loaded and some of it's direct
   * descendants are missing. This can be used to check if comments are missing or just not
   * loaded yet.
   */
  private val fullyLoadedCommentIds = mutableSetOf<CommentId>()

  val switchAccountState = StatefulLiveData<Unit>()

  /**
   * Comments that didn't load by default but were loaded by the user requesting additional comments
   */
  private var supplementaryComments = mutableMapOf<Int, CommentView>()

  private val commentsFetcher = CommentsFetcher(lemmyApiClient)

  var preferences: Preferences = preferenceManager.currentPreferences

  val commentsSortOrderLiveData = MutableLiveData(
    preferences.defaultCommentsSortOrder ?: CommentsSortOrder.Top,
  )

  val postModel = StatefulLiveData<PostModel>()
  val commentNavControlsState = MutableLiveData<CommentNavControlsState?>()

  init {
    state.get<PostRef>("postRef")?.let {
      postOrCommentRef = Either.Left(it)
    }
    state.get<CommentRef>("commentRef")?.let {
      postOrCommentRef = Either.Right(it)
    }

    commentsSortOrderLiveData.observeForever {
      fetchPostData(fetchPostData = false)
    }
    viewModelScope.launch(Dispatchers.Default) {
      accountActionsManager.onCommentActionChanged.collect {
        onCommentActionChanged()
      }
    }
    viewModelScope.launch(Dispatchers.Default) {
      accountManager.currentAccount.collect {
        val account = it as? Account

        if (currentAccountIdOverride == null) {
          onAccountChanged(account)
        }
      }
    }
    viewModelScope.launch(Dispatchers.Default) {
      findInPageQueryFlow
        .debounce(300)
        .collect {
          findInPageQuery.postValue(it)
        }
    }
  }

  private suspend fun onAccountChanged(account: Account?) {
    withContext(Dispatchers.Main) {
      preferences = preferenceManager.currentPreferences

      if (account != null) {
        currentAccountView.value = accountInfoManager.getAccountViewForAccount(account)
      } else {
        currentAccountView.value = null
      }
    }
  }

  val apiInstance: String
    get() = lemmyApiClient.instance

  var initialMaxDepth: Int? = if (preferences.collapseChildCommentsByDefault) {
    1
  } else {
    COMMENTS_DEPTH_MAX
  }

  fun forceAccount(accountId: Long) {
    currentAccountIdOverride = accountId

    lemmyApiClient.forceUseAccount(accountId)

    viewModelScope.launch {
      onAccountChanged(accountManager.getAccountById(accountId))
    }
  }

  fun updateOriginalPostOrCommentRef(postOrCommentRef: Either<PostRef, CommentRef>) {
    selectedCommentId.value = postOrCommentRef.getOrNull()?.id
  }

  fun updatePostOrCommentRef(postOrCommentRef: Either<PostRef, CommentRef>) {
    this.postOrCommentRef = postOrCommentRef

    postOrCommentRef.leftOrNull()?.let {
      this.postRef = it
    }

    onPostOrCommentRefChange.postValue(postOrCommentRef)
  }

  fun switchToNativeInstance() {
    val currentInstance = lemmyApiClient.instance
    val nativeInstance = currentAccountView.value?.account?.instance
      ?: return

    switchAccountState.observeForever(
      object : Observer<StatefulData<Unit>> {
        override fun onChanged(value: StatefulData<Unit>) {
          when (value) {
            is StatefulData.Error -> {
              switchAccountState.removeObserver(this)
              lemmyApiClient.changeInstance(currentInstance)
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
              switchAccountState.removeObserver(this)
            }
          }
        }
      },
    )

    lemmyApiClient.changeInstance(nativeInstance)
    fetchPostData(force = true, switchToNativeInstance = true)
  }

  fun markPostAsRead(postView: PostView) {
    val postView = postView
    postReadManager.markPostAsReadLocal(apiInstance, postView.post.id, read = true)
    duplicatePostsDetector.addReadOrHiddenPost(postView)
  }

  fun fetchPostData(
    fetchPostData: Boolean = true,
    fetchCommentData: Boolean = true,
    force: Boolean = false,
    switchToNativeInstance: Boolean = false,
    markPostAsRead: Boolean = true,
  ): Job? {
    Log.d(
      TAG,
      "fetchPostData(): fetchPostData = $fetchPostData " +
        "fetchCommentData = $fetchCommentData force = $force",
    )

    postOrCommentRef ?: return null

    postModel.setIsLoading()

    return viewModelScope.launch(Dispatchers.Default) {
      _fetchPostData(
        fetchPostData,
        fetchCommentData,
        force,
        switchToNativeInstance,
        markPostAsRead,
      )
    }
  }

  fun switchAccount(account: Account?) {
    val postOrCommentRef = postOrCommentRef ?: return

    val instance = postOrCommentRef.fold(
      { it.instance },
      { it.instance },
    )
    val newInstance = account?.instance ?: DEFAULT_INSTANCE
    val didInstanceChange = instance != newInstance

    if (account?.id == currentAccountView.value?.account?.id) {
      return
    }

    switchAccountState.setIsLoading(context.getString(R.string.switching_instance))

    Log.d(TAG, "Instance changed. Trying to resolve post in new instance.")

    unauthedApiClient.changeInstance(instance)

    viewModelScope.launch(Dispatchers.Default) {
      val linkToResolve = postOrCommentRef
        .fold(
          {
            unauthedApiClient.fetchPost(null, Either.Left(it.id), force = false)
              .fold(
                onSuccess = {
                  Result.success(it.postView.post.ap_id)
                },
                onFailure = {
                  Result.failure(it)
                },
              )
          },
          { commentRef ->
            unauthedApiClient
              .fetchComments(
                null,
                id = Either.Right(commentRef.id),
                sort = CommentSortType.Top,
                force = false,
                maxDepth = 0,
              )
              .fold(
                onSuccess = {
                  val url = it.firstOrNull { it.comment.id == commentRef.id }?.comment?.ap_id
                  if (url != null) {
                    Result.success(url)
                  } else {
                    Result.failure(ObjectResolverFailedException())
                  }
                },
                onFailure = {
                  Result.failure(it)
                },
              )
          },
        )

      accountManager.setCurrentAccount(account)

      linkToResolve
        .fold(
          onSuccess = {
            Log.d(TAG, "Attempting to resolve $linkToResolve on instance $apiInstance")
            lemmyApiClient.resolveObject(it)
          },
          onFailure = {
            Result.failure(it)
          },
        )
        .fold(
          onSuccess = {
            val postView = it.post
            val commentView = it.comment
            val newPostOrCommentRef = if (postView != null) {
              Either.Left(PostRef(newInstance, postView.post.id))
            } else if (commentView != null) {
              Either.Right(CommentRef(newInstance, commentView.comment.id))
            } else {
              null
            }

            if (newPostOrCommentRef != null) {
              updatePostOrCommentRef(newPostOrCommentRef)

              if (didInstanceChange) {
                commentsFlow.value = null
                pendingComments = null
                supplementaryComments.clear()
                newlyPostedCommentId = null
                additionalLoadedCommentIds.clear()
                removedCommentIds.clear()
              }

              withContext(Dispatchers.Main) {
                fetchPostData(fetchPostData = true, force = true)
              }?.join()
            }

            switchAccountState.postValue(Unit)
          },
          onFailure = {
            Log.e(TAG, "Error resolving object.", it)

            switchAccountState.postError(it)
          },
        )
    }
  }

  private suspend fun translatePostToCurrentInstance(): Result<Unit> {
    val postOrCommentRef = postOrCommentRef ?: return Result.success(Unit)
    val currentAccount = currentAccountView.value?.account ?: return Result.success(Unit)

    val instance = postOrCommentRef.fold(
      { it.instance },
      { it.instance },
    )
    val isNativePost = instance.equals(apiInstance, ignoreCase = true)

    if (isNativePost) return Result.success(Unit)

    unauthedApiClient.changeInstance(instance)

    val postView = postViewFlow.value?.getOrNull()
    val linkToResolve = if (postView != null) {
      Result.success(postView.post.ap_id)
    } else {
      postOrCommentRef
        .fold(
          {
            unauthedApiClient.fetchPost(null, Either.Left(it.id), force = false)
              .fold(
                onSuccess = {
                  Result.success(it.postView.post.ap_id)
                },
                onFailure = {
                  Result.failure(it)
                },
              )
          },
          { commentRef ->
            unauthedApiClient
              .fetchComments(
                null,
                id = Either.Right(commentRef.id),
                sort = CommentSortType.Top,
                force = false,
                maxDepth = 0,
              )
              .fold(
                onSuccess = {
                  val url = it.firstOrNull { it.comment.id == commentRef.id }?.comment?.ap_id
                  if (url != null) {
                    Result.success(url)
                  } else {
                    Result.failure(ObjectResolverFailedException())
                  }
                },
                onFailure = {
                  Result.failure(it)
                },
              )
          },
        )
    }

    return linkToResolve
      .fold(
        onSuccess = {
          Log.d(
            TAG,
            "Attempting to resolve $linkToResolve " +
              "on instance $apiInstance",
          )
          lemmyApiClient.resolveObject(it)
        },
        onFailure = {
          Result.failure(it)
        },
      )
      .fold(
        onSuccess = {
          val postView = it.post
          val commentView = it.comment
          val newPostOrCommentRef = if (postView != null) {
            Either.Left(PostRef(currentAccount.instance, postView.post.id))
          } else if (commentView != null) {
            Either.Right(CommentRef(currentAccount.instance, commentView.comment.id))
          } else {
            null
          }

          if (newPostOrCommentRef != null) {
            updatePostOrCommentRef(newPostOrCommentRef)

            commentsFlow.value = null
            pendingComments = null
            supplementaryComments.clear()
            newlyPostedCommentId = null
            additionalLoadedCommentIds.clear()
            removedCommentIds.clear()
          }

          Result.success(Unit)
        },
        onFailure = {
          Log.e(TAG, "Error resolving object.", it)
          Result.failure(it)
        },
      )
  }

  private fun updatePendingComments(
    postOrCommentRef: Either<PostRef, CommentRef>,
    resolveCompletedPendingComments: Boolean,
  ) {
    val sortOrder = requireNotNull(commentsSortOrderLiveData.value).toApiSortOrder()

    viewModelScope.launch {
      updatePendingCommentsInternal(
        postOrCommentRef = postOrCommentRef,
        sortOrder = sortOrder,
        resolveCompletedPendingComments = resolveCompletedPendingComments,
      )

      updateData(wasUpdateForced = false)
    }
  }

  private suspend fun updatePendingCommentsInternal(
    postOrCommentRef: Either<PostRef, CommentRef>,
    sortOrder: CommentSortType,
    resolveCompletedPendingComments: Boolean,
  ) {
    val postRef = postRef
      ?: postOrCommentRef.leftOrNull()
      ?: return

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

          result = commentsFetcher.fetchCommentsWithRetry(
            id = Either.Left(postRef.id),
            sort = sortOrder,
            maxDepth = COMMENTS_DEPTH_MAX,
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
                    "1 completed pending comment was not " +
                      "updated on the server.",
                  )
                  allCommentsUpdated = false
                } else {
                  Log.d(
                    TAG,
                    "1 completed pending comment was updated on the " +
                      "server. New content: '${newComment.comment.content}'",
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

        requireNotNull(result)

        completedPendingComments.forEach { pendingComment ->
          val commentsResult = if (pendingComment.parentId == null) {
            result
          } else {
            fetchMoreCommentsInternal(
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

  private suspend fun updateData(wasUpdateForced: Boolean) = withContext(Dispatchers.Default) {
    Log.d(TAG, "updateData() - pendingComments: ${pendingComments?.size ?: 0} comments: ${commentsFlow.value}")

    val context = ContextCompat.getContextForLanguage(context)
    val postResult = postViewFlow.value ?: return@withContext
    val post = postResult.getOrNull()
    val postError = postResult.exceptionOrNull()
    val commentsResult = commentsFlow.value
    val comments = commentsResult?.getOrNull()
    val crossPosts = getPostResponse?.crossPosts
    val pendingComments = pendingComments
    val supplementaryComments = supplementaryComments
    val postOrCommentRef = postOrCommentRef
    val commentRef: CommentRef? = postOrCommentRef?.getOrNull()
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
      ).buildCommentsTreeListView(
        post = post,
        comments = comments,
        pendingComments = pendingComments,
        supplementaryComments = supplementaryComments,
        removedCommentIds = removedCommentIds,
        fullyLoadedCommentIds = fullyLoadedCommentIds,
        targetCommentRef = postOrCommentRef
          ?.fold(
            { null },
            { it },
          ),
        singleCommentChain = commentRef,
      ),
      crossPosts = crossPosts ?: listOf(),
      newlyPostedCommentId = newlyPostedCommentId,
      selectedCommentId = selectedCommentId.value
        ?: commentRef?.id,
      isSingleCommentChain = isSingleCommentChain,
      isNativePost = isNativePost(),
      accountInstance = currentAccountView.value?.account?.instance,
      isCommentsLoaded = comments != null,
      commentPath = comments?.firstOrNull()?.comment?.path,
      wasUpdateForced = wasUpdateForced,
      loadCommentError = commentsResult?.exceptionOrNull(),
    )

    postModel.postValue(postModelValue)
  }

  fun isNativePost(): Boolean {
    val postOrCommentRef = postOrCommentRef ?: return true
    val currentAccount = currentAccountView.value ?: return true

    val instance = postOrCommentRef.fold(
      { it.instance },
      { it.instance },
    )
    return instance.equals(currentAccount.account.instance, ignoreCase = true)
  }

  fun fetchMoreComments(parentId: CommentId?, maxDepth: Int? = null, force: Boolean = false) {
    val sortOrder = requireNotNull(commentsSortOrderLiveData.value).toApiSortOrder()

    viewModelScope.launch {
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

    viewModelScope.launch {
      delay(HIGHLIGHT_COMMENT_MS)

      updateData(wasUpdateForced = false)
    }
  }

  private suspend fun fetchMoreCommentsInternal(
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

        //            val commentChildCount = thisComment?.counts?.child_count ?: 0
        //            if (thisComment != null && commentChildCount > 0 && comments.size == 1 && depthIsMoreThanOne) {
        //                // This comment's child was deleted...
        //                val fakeComment = createFakeDeletedComment(thisComment.comment.path)
        //                removedCommentIds.add(fakeComment.comment.id)
        //                supplementaryComments[fakeComment.comment.id] = fakeComment
        //            }

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

  fun onCommentActionChanged() {
    val postOrCommentRef = postOrCommentRef
    if (postOrCommentRef != null) {
      updatePendingComments(
        postOrCommentRef = postOrCommentRef,
        resolveCompletedPendingComments = true,
      )
    }
  }

  fun setCommentsSortOrder(sortOrder: CommentsSortOrder) {
    commentsSortOrderLiveData.value = sortOrder
  }

  fun toggleCommentNavControls() {
    if (commentNavControlsState.value == null) {
      commentNavControlsState.value = CommentNavControlsState(
        preferences.commentsNavigationFabOffX,
        preferences.commentsNavigationFabOffY,
      )
    } else {
      commentNavControlsState.value = null
    }
  }

  fun setFindInPageQuery(query: String) {
    viewModelScope.launch {
      findInPageQueryFlow.emit(query)
    }
  }

  fun updatePostViewIfNeeded(post: PostView?) {
    post ?: return
    postViewFlow.value = Result.success(post)
  }

  fun fetchCommentPath(instance: String, commentPath: String) {
    val commentIds = commentPath.split(".").map { it.toIntOrNull() }
    val topCommentId = if (commentIds.size > 1) {
      commentIds[1]
    } else {
      return
    } ?: return

    this.postOrCommentRef = Either.Right(CommentRef(instance, topCommentId))

    onPostOrCommentRefChange.postValue(postOrCommentRef)
    fetchPostData()
  }

  private fun trackVisitIfNeeded() {
    val postView = postViewFlow.value?.getOrNull() ?: return

    if (!visitTracked.value) {
      visitTracked.value = true

      tracker.trackEvent(
        instanceId = postView.community.instance_id.toLong(),
        communityRef = postView.community.toCommunityRef(),
        postId = postView.post.id.toLong(),
        commentId = null,
        targetUserId = postView.post.creator_id,
        action = TrackedAction.VIEW,
        nsfw = postView.post.nsfw || postView.community.nsfw,
      )
    }
  }

  private suspend fun _fetchPostData(
    fetchPostData: Boolean = true,
    fetchCommentData: Boolean = true,
    force: Boolean = false,
    switchToNativeInstance: Boolean = false,
    markPostAsRead: Boolean = true,
  ) {
    mutex.withLock {
      var fetchPostData = fetchPostData
      var force = force
      val sortOrder = requireNotNull(commentsSortOrderLiveData.value).toApiSortOrder()
      var translatePostToCurrentInstanceResult: Result<Unit>? = null

      if (switchToNativeInstance) {
        switchAccountState.postIsLoading(context.getString(R.string.switching_instance))

        translatePostToCurrentInstanceResult = translatePostToCurrentInstance()

        translatePostToCurrentInstanceResult
          .onSuccess {
            fetchPostData = true
            force = true
          }
          .onFailure {
            switchAccountState.postError(it)
            return@withLock
          }
      }

      val postOrCommentRef = postOrCommentRef ?: return

      lemmyApiClient.changeInstance(
        postOrCommentRef
          .fold(
            { it.instance },
            { it.instance },
          ),
      )

      val apiInstance = lemmyApiClient.instance
      val postResult: Result<GetPostResponse?> =
        if (!markPostAsRead) {
          Result.failure(RuntimeException("Can't fetch post or else it will be marked as read!"))
        } else if (fetchPostData) {
          postOrCommentRef
            .fold(
              {
                lemmyApiClient.fetchPostWithRetry(Either.Left(it.id), force)
              },
              {
                lemmyApiClient.fetchPostWithRetry(Either.Right(it.id), force)
              },
            )
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

      trackVisitIfNeeded()

      postViewFlow.value?.getOrNull()?.let {
        postRef = PostRef(instance = apiInstance, id = it.post.id)
      }
      updateData(wasUpdateForced = false)

      val commentsResult = if (fetchCommentData) {
        postOrCommentRef
          .fold(
            {
              commentsFetcher.fetchCommentsWithRetry(
                id = Either.Left(it.id),
                sort = sortOrder,
                maxDepth = initialMaxDepth,
                force = force,
              )
            },
            {
              commentsFetcher.fetchCommentsWithRetry(
                id = Either.Right(it.id),
                sort = sortOrder,
                maxDepth = initialMaxDepth,
                force = force,
              )
            },
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
          fetchMoreCommentsInternal(
            parentId = it,
            sortOrder = sortOrder,
            maxDepth = null,
            force = force,
          )
        }
      }

      updatePendingCommentsInternal(postOrCommentRef, sortOrder, true)

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

      translatePostToCurrentInstanceResult
        ?.onSuccess {
          switchAccountState.postIdle()
        }
    }
  }

  class ObjectResolverFailedException : Exception()
}
