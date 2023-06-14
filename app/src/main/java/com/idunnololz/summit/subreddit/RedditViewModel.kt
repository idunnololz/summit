package com.idunnololz.summit.subreddit

import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.idunnololz.summit.reddit.*
import com.idunnololz.summit.reddit_objects.ListingData
import com.idunnololz.summit.scrape.LoaderException
import com.idunnololz.summit.scrape.WebsiteAdapter
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.Utils
import io.reactivex.disposables.CompositeDisposable
import java.lang.RuntimeException

class RedditViewModel : ViewModel() {

    companion object {
        private val TAG = RedditViewModel::class.java.canonicalName
    }

    class LoadedListingData(
        val listingData: ListingData,
        val url: String,
        val pageIndex: Int
    )

    class PageScrollState(
        var isAtBottom: Boolean = false,
        var itemIndex: Int = 0,
        var offset: Int = 0
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readByte() != 0.toByte(),
            parcel.readInt(),
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeByte(if (isAtBottom) 1 else 0)
            parcel.writeInt(itemIndex)
            parcel.writeInt(offset)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<PageScrollState> {
            override fun createFromParcel(parcel: Parcel): PageScrollState {
                return PageScrollState(parcel)
            }

            override fun newArray(size: Int): Array<PageScrollState?> {
                return arrayOfNulls(size)
            }
        }

    }

    private var pagePositions = arrayListOf<PageScrollState>()
    private val disposables = CompositeDisposable()
    private val redditPageLoader = RedditPageLoader()

    val loadedListingData = StatefulLiveData<LoadedListingData>()
    val currentSubreddit = redditPageLoader.currentSubreddit
    val currentPageIndex = MutableLiveData<Int>()
    val subredditChangeObserver = Observer<String> {
        RecentSubredditsManager.instance.addRecentSubreddit(it)
    }

    init {
        redditPageLoader.apply {
            onListingPageLoadStartListener = {
                loadedListingData.setIsLoading()
            }
            onListingPageLoadedListener = { url, pageIndex, adapter ->
                if (adapter.isSuccess()) {
                    val data = adapter.get().data

                    if (data != null) {
                        if (Utils.isUiThread()) {
                            loadedListingData.setValue(
                                LoadedListingData(
                                    data,
                                    url,
                                    pageIndex
                                )
                            )
                        } else {
                            loadedListingData.postValue(
                                LoadedListingData(
                                    data,
                                    url,
                                    pageIndex
                                )
                            )
                        }
                    } else {
                        loadedListingData.postError(LoaderException(WebsiteAdapter.UNKNOWN_ERROR))
                    }
                } else {
                    loadedListingData.postError(LoaderException(adapter.error))
                }
            }
        }
        currentPageIndex.apply {
            value = redditPageLoader.currentPageIndex
        }
        currentSubreddit.observeForever(subredditChangeObserver)
    }

    fun fetchPrevPage(force: Boolean = false) {
        redditPageLoader.fetchPrevPage(force)
        currentPageIndex.value = redditPageLoader.currentPageIndex
    }

    fun fetchNextPage(force: Boolean = false, clearPagePosition: Boolean) {
        if (clearPagePosition) {
            pagePositions = ArrayList(pagePositions.take(redditPageLoader.currentPageIndex + 1))
        }
        redditPageLoader.fetchNextPage(force)
        currentPageIndex.value = redditPageLoader.currentPageIndex
    }

    fun fetchPage(pageIndex: Int) {
        redditPageLoader.fetchPage(pageIndex)
        currentPageIndex.value = redditPageLoader.currentPageIndex
    }

    fun hasNextPage(): Boolean = redditPageLoader.hasNextPage()

    fun fetchStartPage(subreddit: String? = null, force: Boolean = false) {
        redditPageLoader.fetchStartPage(subreddit, force)
        currentPageIndex.value = redditPageLoader.currentPageIndex
    }

    fun fetchCurrentPage(force: Boolean = false) {
        redditPageLoader.fetchCurrentPage(force)
        currentPageIndex.value = redditPageLoader.currentPageIndex
    }

    fun changeToSubreddit(subreddit: String?) {
        redditPageLoader.changeToSubreddit(subreddit)
    }

    fun setPages(pages: List<RedditPageLoader.PageInfo>, pageIndex: Int) {
        redditPageLoader.setPages(pages, pageIndex)
        currentPageIndex.value = pageIndex
    }

    fun createState(): SubredditViewState =
        SubredditViewState(redditPageLoader.createRedditState(), pagePositions)

    fun restoreFromState(state: SubredditViewState?) {
        state ?: return
        redditPageLoader.restoreState(state.subredditState)
        pagePositions = ArrayList(state.pageScrollStates)
    }

    fun getSharedLinkForCurrentPage(): String =
        redditPageLoader.getSharedLinkForCurrentPage()

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

    fun setSortOrder(newSortOrder: RedditSortOrder) {
        if (redditPageLoader.sortOrder == newSortOrder) return

        redditPageLoader.sortOrder = newSortOrder
        redditPageLoader.resetPages()
        pagePositions.clear()
        redditPageLoader.fetchCurrentPage()
        currentPageIndex.value = 0
    }

    fun getCurrentSortOrder(): RedditSortOrder = redditPageLoader.sortOrder

    override fun onCleared() {
        super.onCleared()

        disposables.clear()
        redditPageLoader.destroy()
        currentSubreddit.removeObserver(subredditChangeObserver)
    }
}
