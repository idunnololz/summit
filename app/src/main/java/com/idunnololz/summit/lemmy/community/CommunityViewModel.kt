package com.idunnololz.summit.lemmy.community

import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.Community
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.CommunityState
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.lemmy.RecentCommunityManager
import com.idunnololz.summit.lemmy.post.PostsRepository
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.reddit.*
import com.idunnololz.summit.reddit_objects.ListingData
import com.idunnololz.summit.scrape.LoaderException
import com.idunnololz.summit.scrape.WebsiteAdapter
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.lang.RuntimeException
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val lemmyApiClient: LemmyApiClient,
    private val postsRepository: PostsRepository,
    private val recentCommunityManager: RecentCommunityManager,
) : ViewModel() {

    companion object {
        private val TAG = CommunityViewModel::class.java.canonicalName
    }

    class LoadedListingData(
        val listingData: ListingData,
        val url: String,
        val pageIndex: Int
    )

    class LoadedPostsData(
        val posts: List<PostView>,
        val url: String,
        val pageIndex: Int,
        val hasMore: Boolean,
    )

    @Parcelize
    class PageScrollState(
        var isAtBottom: Boolean = false,
        var itemIndex: Int = 0,
        var offset: Int = 0
    ) : Parcelable

    private var pagePositions = arrayListOf<PageScrollState>()
    private val disposables = CompositeDisposable()

    val loadedListingData = StatefulLiveData<LoadedListingData>()
    val currentCommunity = MutableLiveData<Community>(Community.All())
    val currentPageIndex = MutableLiveData<Int>(0)
    private val communityChangeObserver = Observer<Community> {
        recentCommunityManager.addRecentCommunity(it)
    }
    val loadedPostsData = StatefulLiveData<LoadedPostsData>()

    init {
//        viewModelScope.launch {
//            lemmyApiClient.getCommunity(null, Either.left())
//        }
        currentCommunity.observeForever(communityChangeObserver)

    }

    fun fetchPrevPage(force: Boolean = false) {
        val pageIndex = requireNotNull(currentPageIndex.value)
        if (pageIndex == 0) {
            return
        }

        currentPageIndex.value = pageIndex - 1
        fetchCurrentPageInternal(force)
    }

    fun fetchNextPage(force: Boolean = false, clearPagePosition: Boolean) {
        val pageIndex = requireNotNull(currentPageIndex.value)

        if (clearPagePosition) {
            pagePositions = ArrayList(pagePositions.take(pageIndex + 1))
        }

        currentPageIndex.value = pageIndex + 1

        fetchCurrentPageInternal(force)
    }

    fun fetchPage(pageIndex: Int) {
        currentPageIndex.value = pageIndex
        fetchCurrentPageInternal(force = false)
    }

    fun fetchCurrentPage(force: Boolean = false) {
        fetchCurrentPageInternal(force)
    }

    private fun fetchCurrentPageInternal(force: Boolean) {
        val currentPage = requireNotNull(currentPageIndex.value)
        viewModelScope.launch {
            val result = postsRepository.getPage(currentPage, null, force)
            loadedPostsData.postValue(
                LoadedPostsData(
                    result.posts,
                    postsRepository.getSite(),
                    currentPage,
                    result.hasMore,
                )
            )
        }
    }

    fun changeCommunity(community: Community?) {
        postsRepository.setCommunity(community)
    }

    fun setPages(pages: List<RedditPageLoader.PageInfo>, pageIndex: Int) {
        currentPageIndex.value = pageIndex
    }

    fun createState(): CommunityViewState? {
        return CommunityViewState(
            CommunityState(
                community = currentCommunity.value ?: return null,
                pages = listOf(),
                currentPageIndex = currentPageIndex.value ?: return null,
            ),
            pagePositions
        )
    }

    fun restoreFromState(state: CommunityViewState?) {
        state ?: return
        currentCommunity.value = state.communityState.community
        currentPageIndex.value = state.communityState.currentPageIndex
        pagePositions = ArrayList(state.pageScrollStates)
    }

    fun getSharedLinkForCurrentPage(): String? =
        createState()?.toUrl()

    fun setPagePositionAtTop(pageIndex: Int) {
        getPagePosition(pageIndex).apply {
            isAtBottom = false
            itemIndex = 0
            offset = 0
        }
    }

    fun setPagePositionAtBottom(pageIndex: Int) {
        getPagePosition(pageIndex).isAtBottom = true
    }

    fun setPagePosition(pageIndex: Int, itemIndex: Int = 0, offset: Int) {
        getPagePosition(pageIndex).apply {
            isAtBottom = false
            this.itemIndex = itemIndex
            this.offset = offset
        }
    }

    fun getPagePosition(pageIndex: Int): PageScrollState {
        if (pageIndex < 0) throw RuntimeException("getPagePosition(): Index was -1!")
        if (pageIndex >= pagePositions.size) {
            for (i in 0 until pageIndex + 1 - pagePositions.size) {
                pagePositions.add(PageScrollState())
            }
        }
        return pagePositions[pageIndex]
    }

    fun setSortOrder(newSortOrder: CommunitySortOrder) {
        TODO()
//        if (redditPageLoader.sortOrder == newSortOrder) return
//
//        redditPageLoader.sortOrder = newSortOrder
//        redditPageLoader.resetPages()
//        pagePositions.clear()
//        redditPageLoader.fetchCurrentPage()
//        currentPageIndex.value = 0
    }

    fun getCurrentSortOrder(): CommunitySortOrder = postsRepository.sortOrder

    override fun onCleared() {
        super.onCleared()

        disposables.clear()
        currentCommunity.removeObserver(communityChangeObserver)
    }
}
