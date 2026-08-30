package com.idunnololz.summit.lemmy.post

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.COMMENTS_DEPTH_MAX
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.api.dto.lemmy.CommentSortType
import com.idunnololz.summit.lemmy.CommentNavControlsState
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommentsSortOrder
import com.idunnololz.summit.lemmy.Consts.DEFAULT_INSTANCE
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.localTracking.LocalTracker
import com.idunnololz.summit.localTracking.TrackedAction
import com.idunnololz.summit.models.GetPostResponse
import com.idunnololz.summit.models.PostView
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.arrow.Either
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class PostViewModel @Inject constructor(
  private val context: Application,
  private val lemmyApiClientFactory: AccountAwareLemmyClient.Factory,
  private val accountActionsManager: AccountActionsManager,
  private val accountManager: AccountManager,
  private val accountInfoManager: AccountInfoManager,
  private val preferenceManager: PreferenceManager,
  private val state: SavedStateHandle,
  private val unauthedApiClient: LemmyApiClient,
  val queryMatchHelper: QueryMatchHelper,
  private val tracker: LocalTracker,
  private val postAndCommentsLoaderFactory: PostAndCommentsLoader.Factory
) : ViewModel() {

  companion object {
    private const val TAG = "PostViewModel"

    const val HIGHLIGHT_COMMENT_MS = 3_500L
  }

  var initialMaxDepth: Int?
    get() = postAndCommentsLoader?.initialMaxDepth
    set(value) {
      postAndCommentsLoader?.initialMaxDepth = value ?: return
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
  val errorFlow = MutableSharedFlow<PostAndCommentsLoader.ActionError>()

  var initialPostView: PostView? = null

  private val findInPageQueryFlow = MutableStateFlow<String>("")

  private val visitTracked = state.getMutableStateFlow("visitTracked", false)

  private var postAndCommentsLoader: PostAndCommentsLoader? = null

  val switchAccountState = StatefulLiveData<Unit>()

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
      postAndCommentsLoader?.commentsSortOrder = it
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

  fun fetchPostData(
    fetchPostData: Boolean = true,
    fetchCommentData: Boolean = true,
    force: Boolean = false,
    switchToNativeInstance: Boolean = false,
    markPostAsRead: Boolean = true,
  ): Job {
    return viewModelScope.launch {
      var fetchPostData = fetchPostData
      var force = force
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
            return@launch
          }
      }

      val postOrCommentRef = postOrCommentRef ?: return@launch
      var getPostResponse: GetPostResponse? = postAndCommentsLoader?.getPostResponse

      lemmyApiClient.changeInstance(
        postOrCommentRef
          .fold(
            { it.instance },
            { it.instance },
          ),
      )

      val postResult: Result<GetPostResponse?> =
        if (!markPostAsRead) {
          Result.failure(RuntimeException("Can't fetch post or else it will be marked as read!"))
        } else if (fetchPostData) {
          if (force || getPostResponse == null) {
            postOrCommentRef
              .fold(
                {
                  withContext(Dispatchers.IO) {
                    lemmyApiClient.fetchPostWithRetry(Either.Left(it.id), force)
                  }
                },
                {
                  withContext(Dispatchers.IO) {
                    lemmyApiClient.fetchPostWithRetry(Either.Right(it.id), force)
                  }
                },
              )
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
      loadedPostView?.post?.id?.let {
        postRef = PostRef(instance = apiInstance, id = it)
      }

      ensurePostAndCommentsLoader(postResult.getOrNull())

      postAndCommentsLoader?.fetchPostData(
        fetchPostData = fetchPostData,
        fetchCommentData = fetchCommentData,
        force = force,
        markPostAsRead = markPostAsRead,
      )?.join()

      translatePostToCurrentInstanceResult
        ?.onSuccess {
          switchAccountState.postIdle()
        }
    }
  }

  fun ensurePostAndCommentsLoader(getPostResponse: GetPostResponse?) {
    if (postAndCommentsLoader != null) {
      return
    }

    val postRef = postRef ?: return

    postAndCommentsLoader = postAndCommentsLoaderFactory.create(
      context = context,
      coroutineScope = viewModelScope,
      initialMaxDepth = if (preferences.collapseChildCommentsByDefault) {
        1
      } else {
        COMMENTS_DEPTH_MAX
      },
      defaultSortOrder = preferences.defaultCommentsSortOrder ?: CommentsSortOrder.Top,
      postRef = postRef,
      commentRef = postOrCommentRef?.getOrNull(),
      initialPostView = if (initialPostView?.post?.id == postRef.id) {
        initialPostView
      } else {
        null
      },
      preferUserDisplayName = preferences.preferUserDisplayName,
      lemmyApiClient = lemmyApiClient,
      getPostResponse = getPostResponse,
      currentAccountView = currentAccountView,
      selectedCommentId = selectedCommentId.value,
      callback = object : PostAndCommentsLoader.Callback {
        override fun onPostViewLoaded(postView: PostView) {
          trackVisitIfNeeded(postView)
        }

        override fun onPostModelChanged(postModel: StatefulData<PostModel>) {
          Log.d(TAG, "onPostModelChanged() - ${postModel::class.simpleName}")
          when (postModel) {
            is StatefulData.Error<*> -> {
              this@PostViewModel.postModel.postError(postModel.error)
            }
            is StatefulData.Loading<*> -> {
              this@PostViewModel.postModel.postIsLoading()
            }
            is StatefulData.NotStarted<*> -> {
              this@PostViewModel.postModel.postIdle()
            }
            is StatefulData.Success -> {
              this@PostViewModel.postModel.postValue(postModel.data)
            }
          }
        }

        override fun onError(error: PostAndCommentsLoader.ActionError) {
          viewModelScope.launch {
            errorFlow.emit(error)
          }
        }
      }
    )
  }

  val apiInstance: String
    get() = lemmyApiClient.instance

  fun forceAccount(accountId: Long) {
    currentAccountIdOverride = accountId

    lemmyApiClient.forceUseAccount(accountId)

    viewModelScope.launch {
      onAccountChanged(accountManager.getAccountById(accountId))
    }
  }

  fun updateOriginalPostOrCommentRef(postOrCommentRef: Either<PostRef, CommentRef>) {
    val commentId = postOrCommentRef.getOrNull()?.id
    selectedCommentId.value = commentId
    postAndCommentsLoader?.selectedCommentId = commentId
  }

  fun updatePostOrCommentRef(newPostOrCommentRef: Either<PostRef, CommentRef>) {
    this.postOrCommentRef = newPostOrCommentRef

    newPostOrCommentRef.leftOrNull()?.let {
      this.postRef = it
    }

    onPostOrCommentRefChange.postValue(newPostOrCommentRef)
    clearPostAndCommentsLoader()
  }

  private fun clearPostAndCommentsLoader() {
    postAndCommentsLoader?.cancel()
    postAndCommentsLoader = null
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
                clearPostAndCommentsLoader()
              }

              withContext(Dispatchers.Main) {
                fetchPostData(fetchPostData = true, force = true)
              }.join()
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

    val postView = postAndCommentsLoader?.postView
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
            clearPostAndCommentsLoader()
          }

          Result.success(Unit)
        },
        onFailure = {
          Log.e(TAG, "Error resolving object.", it)
          Result.failure(it)
        },
      )
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

  fun onCommentActionChanged() {
    val postOrCommentRef = postOrCommentRef
    if (postOrCommentRef != null) {
      postAndCommentsLoader?.updatePendingComments(
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

  fun fetchCommentPath(instance: String, commentPath: String) {
    Log.d(TAG, "fetchCommentPath() $instance $commentPath")

    val commentIds = commentPath.split(".").map { it.toIntOrNull() }
    val topCommentId = if (commentIds.size > 1) {
      commentIds[1]
    } else {
      return
    } ?: return

    updatePostOrCommentRef(Either.Right(CommentRef(instance, topCommentId)))
    fetchPostData()
  }

  fun fetchMoreComments(commentId: CommentId, maxDepth: Int? = null, force: Boolean = false) {
    postAndCommentsLoader?.fetchMoreComments(commentId, maxDepth, force)
  }

  fun resetNewlyPostedComment() {
    postAndCommentsLoader?.resetNewlyPostedComment()
  }

  private fun trackVisitIfNeeded(postView: PostView) {
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

  class ObjectResolverFailedException : Exception()
}
