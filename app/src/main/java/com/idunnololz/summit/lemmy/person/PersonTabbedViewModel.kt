package com.idunnololz.summit.lemmy.person

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityModeratorView
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommentListEngine
import com.idunnololz.summit.lemmy.CommentPageResult
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.LoadedPostsData
import com.idunnololz.summit.lemmy.community.PostListEngine
import com.idunnololz.summit.lemmy.community.PostLoadError
import com.idunnololz.summit.lemmy.community.ViewPagerController
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.toErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonTabbedViewModel @Inject constructor(
    private val context: Application,
    private val apiClient: AccountAwareLemmyClient,
    private val state: SavedStateHandle,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val directoryHelper: DirectoryHelper,
) : ViewModel(), ViewPagerController.PostViewPagerViewModel {

    companion object {
        private const val PAGE_SIZE = 10

        private const val TAG = "PersonTabbedViewModel"
    }

    val personData = StatefulLiveData<PersonDetailsData>()
    val postsState = StatefulLiveData<UpdateInfo>()
    val commentsState = StatefulLiveData<UpdateInfo>()

    val postListEngine = PostListEngine(
        infinity = true,
        autoLoadMoreItems = true,
        coroutineScopeFactory = coroutineScopeFactory,
        directoryHelper = directoryHelper,
    )
    var commentListEngine = CommentListEngine()

    var sortType: SortType = SortType.New
        set(value) {
            field = value

            reset()
            fetchPage(pageIndex = 0, isPeronInfoFetch = true, force = true)
        }

    val instance: String
        get() = apiClient.instance

    override var lastSelectedPost: PostRef? = null
    override val viewPagerAdapter = ViewPagerController.ViewPagerAdapter()

    private var personRef: PersonRef? = null
    private var fetchingPages = mutableSetOf<Int>()

    fun fetchPersonIfNotDone(personRef: PersonRef) {
        if (personData.valueOrNull != null) return

        this.personRef = personRef
        fetchPage(0, isPeronInfoFetch = true)
    }

    fun fetchPage(pageIndex: Int, isPeronInfoFetch: Boolean = false, force: Boolean = false) {
        if (fetchingPages.contains(pageIndex)) {
            return
        }

        Log.d(TAG, "Fetching page $pageIndex")

        fetchingPages.add(pageIndex)

        val personRef = personRef ?: return

        if (isPeronInfoFetch) {
            personData.setIsLoading()
        }
        postsState.setIsLoading()
        commentsState.setIsLoading()

        viewModelScope.launch {
            if (force) {
                reset()
            }

            val result = when (personRef) {
                is PersonRef.PersonRefByName -> {
                    apiClient.fetchPersonByNameWithRetry(
                        name = personRef.fullName,
                        page = pageIndex.toLemmyPageIndex(),
                        limit = PAGE_SIZE,
                        force = force,
                        sortType = sortType,
                    )
                }
            }

            result
                .onSuccess {
                    if (isPeronInfoFetch) {
                        personData.postValue(
                            PersonDetailsData(
                                it.person_view,
                                it.comments,
                                it.posts,
                                it.moderates,
                            ),
                        )
                    }

                    if (postListEngine.hasMore || force) {
                        postListEngine.addPage(
                            LoadedPostsData(
                                it.posts,
                                apiClient.instance,
                                pageIndex,
                                it.posts.size == PAGE_SIZE,
                            ),
                        )
                        postListEngine.createItems()
                    }
                    if (commentListEngine.hasMore || force) {
                        commentListEngine.addComments(
                            CommentPageResult(
                                it.comments,
                                apiClient.instance,
                                pageIndex,
                                it.comments.size == PAGE_SIZE,
                                null,
                            ),
                        )
                    }

                    postsState.postValue(
                        UpdateInfo(
                            isReset = force && pageIndex == 0,
                        ),
                    )
                    commentsState.postValue(
                        UpdateInfo(
                            isReset = force && pageIndex == 0,
                        ),
                    )

                    fetchingPages.remove(pageIndex)
                }
                .onFailure {
                    if (postListEngine.hasMore || force) {
                        postListEngine.addPage(
                            LoadedPostsData(
                                listOf(),
                                apiClient.instance,
                                pageIndex,
                                false,
                                error = PostLoadError(
                                    errorCode = 0,
                                    errorMessage = it.toErrorMessage(context),

                                    isRetryable = true,
                                    isLoading = false,
                                ),
                            ),
                        )
                        postListEngine.createItems()
                    }
                    if (commentListEngine.hasMore || force) {
                        commentListEngine.addComments(
                            CommentPageResult(
                                listOf(),
                                apiClient.instance,
                                pageIndex,
                                false,
                                it,
                            ),
                        )
                    }

                    if (isPeronInfoFetch) {
                        personData.postError(it)
                    }
                    postsState.postError(it)
                    commentsState.postError(it)

                    fetchingPages.remove(pageIndex)
                }
        }
    }

    fun fetchNextCommentPage() {
        fetchPage(commentListEngine.nextPage, false)
    }

    private fun reset() {
        postListEngine.clearPages()
        commentListEngine.clear()
    }

    data class PersonDetailsData(
        val personView: PersonView,
        val comments: List<CommentView>,
        val posts: List<PostView>,
        val moderates: List<CommunityModeratorView>,
    )

    private fun Int.toLemmyPageIndex() =
        this + 1 // lemmy pages are 1 indexed

    data class UpdateInfo(
        val isReset: Boolean,
    )
}
