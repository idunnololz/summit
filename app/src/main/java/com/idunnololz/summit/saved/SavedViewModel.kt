package com.idunnololz.summit.saved

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommentListEngine
import com.idunnololz.summit.lemmy.CommentPageResult
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.LoadedPostsData
import com.idunnololz.summit.lemmy.community.PostListEngine
import com.idunnololz.summit.lemmy.community.PostLoadError
import com.idunnololz.summit.lemmy.community.ViewPagerController
import com.idunnololz.summit.lemmy.person.PersonTabbedViewModel
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
    private val state: SavedStateHandle,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val offlineManager: OfflineManager,
) : ViewModel(), ViewPagerController.PostViewPagerViewModel {

    companion object {
        private const val TAG = "SavedViewModel"

        private const val PAGE_SIZE = 20
    }

    val currentAccountView = MutableLiveData<AccountView?>()

    val postsState = StatefulLiveData<Unit>()
    val commentsState = StatefulLiveData<Unit>()

    val postListEngine = PostListEngine(infinity = true, coroutineScopeFactory, offlineManager)
    var commentListEngine = CommentListEngine()

    val instance: String
        get() = apiClient.instance

    private var fetchingPostPages = mutableSetOf<Int>()
    private var fetchingCommentPages = mutableSetOf<Int>()

    override var lastSelectedPost: PostRef? = null
    override val viewPagerAdapter = ViewPagerController.ViewPagerAdapter()

    init {
        fetchPostPage(0, false)
        fetchCommentPage(0, false)

        viewModelScope.launch {
            accountManager.currentAccountOnChange.collect {
                delay(10) // just in case it takes a second for the api client to update...

                fetchingPostPages.clear()
                fetchingCommentPages.clear()

                postListEngine.clearPages()
                commentListEngine.clear()

                fetchPostPage(0, false)
                fetchCommentPage(0, false)
            }
        }

        viewModelScope.launch {
            accountInfoManager.currentFullAccount.collect {
                withContext(Dispatchers.Main) {
                    if (it != null) {
                        currentAccountView.value = accountInfoManager.getAccountViewForAccount(it.account)
                    } else {
                        currentAccountView.value = null
                    }
                }
            }
        }
    }

    fun fetchPostPage(pageIndex: Int, force: Boolean) {
        if (fetchingPostPages.contains(pageIndex)) {
            return
        }

        Log.d(TAG, "Fetching page $pageIndex")

        fetchingPostPages.add(pageIndex)

        viewModelScope.launch {
            apiClient.fetchSavedPostsWithRetry(pageIndex.toLemmyPageIndex(), PAGE_SIZE, force)
                .onSuccess {
                    if (postListEngine.hasMore || force) {
                        postListEngine.addPage(
                            LoadedPostsData(
                                it,
                                apiClient.instance,
                                pageIndex,
                                it.size == PAGE_SIZE,
                            )
                        )
                        postListEngine.createItems()
                    }

                    postsState.postValue(Unit)

                    fetchingPostPages.remove(pageIndex)
                }
                .onFailure {
                    if (postListEngine.hasMore || force) {
                        postListEngine.addPage(LoadedPostsData(
                            listOf(),
                            apiClient.instance,
                            pageIndex,
                            false,
                            error = PostLoadError(0)
                        ))
                        postListEngine.createItems()
                    }

                    postsState.postError(it)

                    fetchingPostPages.remove(pageIndex)
                }
        }
    }

    fun fetchCommentPage(pageIndex: Int, force: Boolean = false) {
        if (fetchingCommentPages.contains(pageIndex)) {
            return
        }

        Log.d(TAG, "Fetching page $pageIndex")

        fetchingCommentPages.add(pageIndex)

        viewModelScope.launch {
            apiClient.fetchSavedCommentsWithRetry(pageIndex.toLemmyPageIndex(), PAGE_SIZE, force)
                .onSuccess {
                    if (commentListEngine.hasMore || force) {
                        commentListEngine.addComments(CommentPageResult(
                            it,
                            apiClient.instance,
                            pageIndex,
                            it.size == PAGE_SIZE,
                            null,
                        ))
                    }

                    commentsState.postValue(Unit)

                    fetchingCommentPages.remove(pageIndex)
                }
                .onFailure {
                    if (commentListEngine.hasMore || force) {
                        commentListEngine.addComments(CommentPageResult(
                            listOf(),
                            apiClient.instance,
                            pageIndex,
                            false,
                            it,
                        ))
                    }

                    commentsState.postError(it)

                    fetchingCommentPages.remove(pageIndex)
                }
        }
    }

    private fun Int.toLemmyPageIndex() =
        this + 1 // lemmy pages are 1 indexed

    fun onSavePostChanged(it: PostView) {
        if (!it.saved) {
            postListEngine.removePost(it.post.id)
            postListEngine.createItems()
            postsState.postValue(Unit)
        }
    }

    fun onSaveCommentChanged(it: CommentView) {
        if (!it.saved) {
            commentListEngine.removeComment(it.comment.id)
            commentsState.postValue(Unit)
        }
    }
}