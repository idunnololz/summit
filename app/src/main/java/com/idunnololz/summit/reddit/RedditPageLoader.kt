package com.idunnololz.summit.reddit

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.idunnololz.summit.scrape.WebsiteAdapterLoader
import com.idunnololz.summit.reddit_objects.ListingItem
import com.idunnololz.summit.reddit_objects.ListingObject
import com.idunnololz.summit.reddit_website_adapter.RedditListingWebsiteAdapter
import com.idunnololz.summit.reddit_website_adapter.RedditObjectsWebsiteAdapter
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.Utils

class RedditPageLoader {

    companion object {
        private const val CACHE_LIFE_MS = 1000L * 60 * 60 * 2 // 2 hours

        private const val TAG = "RedditPageLoader"
    }

    class PageInfo(
        val pageId: String,
        var flags: Int
    )

    private val pages = ArrayList<PageInfo>()
    var currentPageIndex: Int = 0

    var onListingPageLoadStartListener: (() -> Unit)? = null
    var onListingPageLoadedListener: ((url: String, pageIndex: Int, RedditListingWebsiteAdapter) -> Unit)? =
        null

    var onPostLoadStartListener: (() -> Unit)? = null
    var onPostLoadedListener: ((RedditObjectsWebsiteAdapter) -> Unit)? = null

    val defaultSubreddit = PreferenceUtil.getDefaultPage()
    private var _currentSubreddit: String = defaultSubreddit
        private set

    val currentSubreddit = MutableLiveData<String>()

    private var loader: WebsiteAdapterLoader? = null

    private val pageMemoryCache = redditPageMemoryCache

    var sortOrder: RedditSortOrder = RedditSortOrder.HotOrder

    init {
        popAllPages()
    }

    fun setPages(pages: List<PageInfo>, pageIndex: Int) {
        require(pages.size > pageIndex) { "Page index must be greater than page length. Index: $pageIndex. Pages: ${pages.size}" }

        currentPageIndex = pageIndex
        this.pages.clear()
        this.pages.addAll(pages)
    }

    fun fetchPrevPage(force: Boolean = false) {
        if (currentPageIndex == 0) return

        currentPageIndex--
        if (currentPageIndex == 0) {
            fetchStartPage(force = force)
        } else {
            fetchCurrentPage(force)
        }
    }

    fun fetchNextPage(force: Boolean = false) {
        moveToNextPage()
        fetchCurrentPage(force)
    }

    fun fetchPage(pageIndex: Int, force: Boolean = false) {
        if (pageIndex >= pages.size) return

        currentPageIndex = pageIndex
        fetchCurrentPage(force)
    }

    /**
     * Should only be used by offline manager
     */
    fun moveToNextPage() {
        if (currentPageIndex + 1 == pages.size) return

        currentPageIndex++
    }

    fun fetchStartPage(subreddit: String? = null, force: Boolean = false) {
        currentPageIndex = 0

        if (subreddit != null) {
            updateSubreddit(subreddit)
        }

        fetchCurrentPage(force)
    }

    fun resetPages() {
        currentPageIndex = 0
        popAllPages()
    }

    fun changeToSubreddit(subreddit: String? = null) {
        resetPages()

        updateSubreddit(subreddit)
    }

    fun fetchCurrentPage(force: Boolean = false) {
        fetchPage(force)
    }

    fun fetchPost(url: String, sortOrder: CommentsSortOrder? = null, force: Boolean = false) {
        fetchPosts(
            urls = listOf(url),
            sortOrder = sortOrder,
            force = force
        )
    }

    fun fetchPostsFromListingItems(
        listingItems: List<ListingItem>,
        sortOrder: CommentsSortOrder? = null,
        force: Boolean = false
    ) {
        fetchPosts(
            urls = listingItems.map { "https://oauth.reddit.com/${it.permalink}.json" },
            sortOrder = sortOrder,
            force = force
        )
    }

    fun fetchPosts(
        urls: List<String>,
        sortOrder: CommentsSortOrder? = null,
        force: Boolean = false
    ) {
        loader?.destroy()

        onPostLoadStartListener?.invoke()

        val completeUrls = urls.map { url ->
            if (sortOrder != null) {
                Uri.parse(url)
                    .buildUpon()
                    .appendQueryParameter("sort", sortOrder.key)
                    .build()
                    .toString()
            } else {
                url
            }
        }

        loader = WebsiteAdapterLoader().apply {
            for (url in completeUrls) {
                add(
                    RedditObjectsWebsiteAdapter(),
                    url,
                    keyFromPostUrl(url)
                )
                Log.d("HAHA", "KEY: ${keyFromPostUrl(url)}")
            }
            setOnEachAdapterLoadedListener {
                if (it is RedditObjectsWebsiteAdapter) {
                    onPostLoadedListener?.invoke(it)
                }
            }
        }.load(forceRefetch = force)
    }

    private fun keyFromPostUrl(url: String): String =
        "post:" + Utils.hashSha256(Uri.parse(url).let {
            it.pathSegments.joinToString(separator = "/") + "?" + it.query
        })

    fun getSharedLinkForCurrentPage(): String =
        getUrlForPage(
            extension = "",
            sortOrder = sortOrder,
            after = pages[currentPageIndex].pageId,
            sharable = true
        )

    fun createRedditState(): SubredditState =
        SubredditState(pages, currentPageIndex, _currentSubreddit, getSharedLinkForCurrentPage())

    fun restoreState(state: SubredditState?) {
        state ?: return

        setPages(state.pages, state.currentPageIndex)
    }

    fun destroy() {
        loader?.destroy()
    }

    private fun popAllPages() {
        pages.clear()
        pages.add(PageInfo("", 0))
    }

    private fun getUrlForPage(
        after: String,
        sortOrder: RedditSortOrder,
        extension: String = ".json",
        sharable: Boolean = false
    ): String {
        val baseUrl = if (sharable) {
            "https://www.reddit.com"
        } else {
            "https://oauth.reddit.com"
        }

        val sortOrderSuffix = when (sortOrder) {
            RedditSortOrder.HotOrder -> ""
            RedditSortOrder.NewOrder -> "new/"
            RedditSortOrder.RisingOrder -> "rising/"
            is RedditSortOrder.TopOrder -> "top/"
        }

        val uriBuilder = Uri.parse("$baseUrl/${_currentSubreddit}/${sortOrderSuffix}$extension")
            .buildUpon()
            .also {
                if (sortOrder is RedditSortOrder.TopOrder) {
                    when
                            (sortOrder.timeFrame) {
                        RedditSortOrder.TimeFrame.NOW -> {
                            it.appendQueryParameter("t", "hour")
                        }
                        RedditSortOrder.TimeFrame.TODAY -> {
                        }
                        RedditSortOrder.TimeFrame.THIS_WEEK -> {
                            it.appendQueryParameter("t", "week")
                        }
                        RedditSortOrder.TimeFrame.THIS_MONTH -> {
                            it.appendQueryParameter("t", "month")
                        }
                        RedditSortOrder.TimeFrame.THIS_YEAR -> {
                            it.appendQueryParameter("t", "year")
                        }
                        RedditSortOrder.TimeFrame.ALL_TIME -> {
                            it.appendQueryParameter("t", "all")
                        }
                    }
                }
                if (after.isNotEmpty()) {
                    it.appendQueryParameter("after", after)
                }
            }
        return uriBuilder.toString()
    }

    private fun updateSubreddit(subreddit: String?) {
        if (subreddit == null) {
            Log.d(TAG, "Subreddit is null. Defaulting to $defaultSubreddit")
            _currentSubreddit = RedditUtils.normalizeSubredditPath(defaultSubreddit)
            return
        }

        _currentSubreddit = RedditUtils.normalizeSubredditPath(subreddit)
    }

    private fun fetchPage(force: Boolean = false) {
        val currentPageIndex = currentPageIndex
        val after = pages[currentPageIndex].pageId

        if (currentPageIndex == 0) {
            popAllPages()
        }

        currentSubreddit.postValue(_currentSubreddit)

        val url = getUrlForPage(sortOrder = sortOrder, after = after)
        onListingPageLoadStartListener?.invoke()

        // rpc = reddit page cache
        val key = "__rpc_${_currentSubreddit.replace("/", ".")}_${sortOrder.getKey()}_${after}__"

        fun onLoaded(adapter: RedditListingWebsiteAdapter) {
            if (adapter.isSuccess()) {
                val data = adapter.get().data

                if (data != null) {
                    if (currentPageIndex + 1 == pages.size && data.after != null) {
                        pages.add(PageInfo(data.after, 0))
                    }
                }
                pageMemoryCache.put(key, CachedObject(System.currentTimeMillis(), adapter.get()))
            }
            onListingPageLoadedListener?.invoke(url, currentPageIndex, adapter)
        }

        val adapter = RedditListingWebsiteAdapter()
        val cached = pageMemoryCache.get(key)
        if (!force && cached != null && System.currentTimeMillis() < cached.ts + CACHE_LIFE_MS) {
            adapter.set(cached.o as ListingObject)
            onLoaded(adapter)
            return
        }

        loader = WebsiteAdapterLoader().apply {
            add(
                adapter = adapter,
                url = url,
                cacheKey = key,
                cacheLifetimeMs = CACHE_LIFE_MS
            )
            setOnEachAdapterLoadedListener { adapter ->
                if (adapter is RedditListingWebsiteAdapter) {
                    onLoaded(adapter)
                }
            }
        }.load(forceRefetch = force)
    }

    fun isSubredditSinglePage(): Boolean = currentPageIndex == 0 && pages.size == 1

    fun hasNextPage(): Boolean = currentPageIndex < pages.size
    fun addPageFlags(pageIndex: Int, flags: Int) {
        pages[pageIndex].flags = pages[pageIndex].flags or flags
    }

    fun clearPageFlags(pageIndex: Int, flags: Int) {
        pages[pageIndex].flags = pages[pageIndex].flags and flags.inv()
    }
}