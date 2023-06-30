package com.idunnololz.summit.lemmy.community

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.CommunityState
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.RecentCommunityManager
import com.idunnololz.summit.lemmy.PostsRepository
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.assertMainThread
import com.squareup.moshi.JsonClass
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import okhttp3.Dispatcher
import java.lang.RuntimeException
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val postsRepository: PostsRepository,
    private val recentCommunityManager: RecentCommunityManager,
    private val accountManager: AccountManager,
    private val userCommunitiesManager: UserCommunitiesManager,
    private val accountActionsManager: AccountActionsManager,
) : ViewModel() {

    companion object {
        private val TAG = CommunityViewModel::class.java.canonicalName
    }

    class LoadedPostsData(
        val posts: List<PostView>,
        val instance: String,
        val pageIndex: Int,
        val hasMore: Boolean,
    )

    @Parcelize
    @JsonClass(generateAdapter = true)
    class PageScrollState(
        var isAtBottom: Boolean = false,
        var itemIndex: Int = 0,
        var offset: Int = 0
    ) : Parcelable

    private var pagePositions = arrayListOf<PageScrollState>()

    val currentCommunityRef = MutableLiveData<CommunityRef>(CommunityRef.All())
    val currentPageIndex = MutableLiveData(0)
    private val communityRefChangeObserver = Observer<CommunityRef> {
        recentCommunityManager.addRecentCommunity(it)
    }
    val loadedPostsData = StatefulLiveData<LoadedPostsData>()

    val voteUiHandler = accountActionsManager.voteUiHandler

    val instance: String
        get() = postsRepository.instance

    var lastSelectedPost: PostRef? = null

    val viewPagerAdapter = ViewPagerController.ViewPagerAdapter()

    init {
        currentCommunityRef.observeForever(communityRefChangeObserver)

        accountManager.addOnAccountChangedListener(object : AccountManager.OnAccountChangedListener {
            override suspend fun onAccountChanged(newAccount: Account?) {
                postsRepository.reset()
            }
        })

        viewModelScope.launch {
            accountManager.currentAccountOnChange
                .collect {
                    withContext(Dispatchers.Main) {
                        reset()
                    }
                }
        }
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
        loadedPostsData.setIsLoading()

        val currentPage = requireNotNull(currentPageIndex.value)
        viewModelScope.launch {
            val result = postsRepository.getPage(
                currentPage, force)

            result
                .onSuccess {
                    loadedPostsData.postValue(
                        LoadedPostsData(
                            posts = it.posts,
                            instance = it.instance,
                            pageIndex = currentPage,
                            hasMore = it.hasMore,
                        )
                    )
                }
                .onFailure {
                    loadedPostsData.postError(it)
                }
        }
    }

    fun changeCommunity(communityRef: CommunityRef?) {
        if (communityRef == null) {
            return
        }

        if (communityRef is CommunityRef.Subscribed && accountManager.currentAccount.value == null) {
            return
        }

        val communityRefSafe: CommunityRef = communityRef

        currentCommunityRef.value = communityRefSafe
        postsRepository.setCommunity(communityRef)
    }

    fun createState(): CommunityViewState? {
        return CommunityViewState(
            CommunityState(
                communityRef = currentCommunityRef.value ?: return null,
                pages = listOf(),
                currentPageIndex = currentPageIndex.value ?: return null,
            ),
            pagePositions
        )
    }

    fun restoreFromState(state: CommunityViewState?) {
        state ?: return
        currentCommunityRef.value = state.communityState.communityRef
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
        postsRepository.sortOrder = newSortOrder

        reset()
    }

    fun getCurrentSortOrder(): CommunitySortOrder = postsRepository.sortOrder

    fun setDefaultPage(currentCommunityRef: CommunityRef) {
        viewModelScope.launch {
            userCommunitiesManager.setDefaultPage(currentCommunityRef)
        }
    }

    fun resetToAccountInstance() {
        val account = accountManager.currentAccount.value ?: return
        changeCommunity(CommunityRef.All(account.instance))

        reset()
    }

    override fun onCleared() {
        super.onCleared()

        currentCommunityRef.removeObserver(communityRefChangeObserver)
    }
    private fun reset() {
        assertMainThread()

        loadedPostsData.setValue(LoadedPostsData(
            posts = listOf(),
            instance = postsRepository.instance,
            pageIndex = 0,
            hasMore = false
        ))
        currentPageIndex.value = 0
        setPagePositionAtTop(0)
        fetchCurrentPage()
    }

    data class CurrentCommunity(
        private val communityRef: CommunityRef,
    )
}
