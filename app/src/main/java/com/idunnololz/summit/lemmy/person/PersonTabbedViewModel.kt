package com.idunnololz.summit.lemmy.person

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityModeratorView
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.LoadedPostsData
import com.idunnololz.summit.lemmy.community.PostListEngine
import com.idunnololz.summit.lemmy.community.ViewPagerController
import com.idunnololz.summit.lemmy.post.PostViewModel
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonTabbedViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val state: SavedStateHandle,
) : ViewModel(), ViewPagerController.PostViewPagerViewModel {

    companion object {
        private const val PAGE_SIZE = 10

        private const val TAG = "PersonTabbedViewModel"
    }

    data class CommentPageResult(
        val comments: List<CommentView>,
        val instance: String,
        val pageIndex: Int,
        val hasMore: Boolean,
        val error: Throwable?,
    )

    val personData = StatefulLiveData<PersonDetailsData>()
    val postsState = StatefulLiveData<Unit>()
    val commentsState = StatefulLiveData<Unit>()

    val postListEngine = PostListEngine(infinity = true, state)
    var commentPages: List<CommentPageResult> = listOf()

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

    fun fetchPage(pageIndex: Int, isPeronInfoFetch: Boolean = false) {
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
            val result = when (personRef) {
                is PersonRef.PersonRefByName -> {
                    apiClient.fetchPersonByNameWithRetry(
                        name = personRef.fullName,
                        page = pageIndex.toLemmyPageIndex(),
                        limit = PAGE_SIZE
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
                            )
                        )
                    }

                    if (postListEngine.pages.lastOrNull()?.hasMore != false) {
                        postListEngine.addPage(LoadedPostsData(
                            it.posts,
                            apiClient.instance,
                            pageIndex,
                            it.posts.size == PAGE_SIZE,
                        ))
                        postListEngine.createItems()
                    }
                    if (commentPages.lastOrNull()?.hasMore != false) {
                        addComments(CommentPageResult(
                            it.comments,
                            apiClient.instance,
                            pageIndex,
                            it.posts.size == PAGE_SIZE,
                            null,
                        ))
                    }

                    postsState.postValue(Unit)
                    commentsState.postValue(Unit)

                    fetchingPages.remove(pageIndex)
                }
                .onFailure {

                    if (postListEngine.pages.lastOrNull()?.hasMore != false) {
                        postListEngine.addPage(LoadedPostsData(
                            listOf(),
                            apiClient.instance,
                            pageIndex,
                            false,
                            error = it
                        ))
                        postListEngine.createItems()
                    }
                    if (commentPages.lastOrNull()?.hasMore != false) {
                        addComments(CommentPageResult(
                            listOf(),
                            apiClient.instance,
                            pageIndex,
                            false,
                            it,
                        ))
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
        val nextPage = (commentPages.lastOrNull()?.pageIndex ?: 0) + 1
        fetchPage(nextPage, false)
    }

    private fun addComments(commentPageResult: CommentPageResult) {
        val newPages = commentPages.toMutableList()
        val existingPage = newPages.getOrNull(commentPageResult.pageIndex)
        if (existingPage != null) {
            newPages[commentPageResult.pageIndex] = existingPage
        } else {
            newPages.add(commentPageResult)
        }

        commentPages = newPages
    }

    data class PersonDetailsData(
        val personView: PersonView,
        val comments: List<CommentView>,
        val posts: List<PostView>,
        val moderates: List<CommunityModeratorView>,
    )

    private fun Int.toLemmyPageIndex() =
        this + 1 // lemmy pages are 1 indexed
}