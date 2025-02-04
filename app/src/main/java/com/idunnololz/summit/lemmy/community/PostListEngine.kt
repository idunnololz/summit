package com.idunnololz.summit.lemmy.community

import android.util.Log
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.api.CommunityBlockedError
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.FilterReason
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.duplicatePostsDetector.DuplicatePostsDetector
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.util.DirectoryHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed interface Item {

    data object HeaderItem : Item

    sealed interface PostItem : Item {
        val fetchedPost: FetchedPost
    }

    data class VisiblePostItem(
        override val fetchedPost: FetchedPost,
        val instance: String,
        val isExpanded: Boolean,
        val isActionExpanded: Boolean,
        val highlight: Boolean,
        val highlightForever: Boolean,
        val pageIndex: Int,
        val isDuplicatePost: Boolean,
    ) : PostItem

    data class FilteredPostItem(
        override val fetchedPost: FetchedPost,
        val instance: String,
        val isExpanded: Boolean,
        val isActionExpanded: Boolean,
        val highlight: Boolean,
        val highlightForever: Boolean,
        val pageIndex: Int,
        val filterReason: FilterReason,
    ) : PostItem

    data class FooterItem(
        val hasMore: Boolean,
        val hasLess: Boolean,
    ) : Item

    data class AutoLoadItem(val pageToLoad: Int) : Item

    data class ManualLoadItem(val pageToLoad: Int) : Item

    data class ErrorItem(val message: String, val pageToLoad: Int, val isLoading: Boolean) : Item

    data class PersistentErrorItem(val exception: Exception) : Item

    data class PageTitle(val pageIndex: Int) : Item

    data object EndItem : Item
    data object FooterSpacerItem : Item
}

class PostListEngine @AssistedInject constructor(
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val directoryHelper: DirectoryHelper,
    private val duplicatePostsDetector: DuplicatePostsDetector,
    private val postReadManager: PostReadManager,

    @Assisted("infinity")
    infinity: Boolean,

    @Assisted("autoLoadMoreItems")
    autoLoadMoreItems: Boolean,

    @Assisted("usePageIndicators")
    usePageIndicators: Boolean = false,
) {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("infinity")
            infinity: Boolean,

            @Assisted("autoLoadMoreItems")
            autoLoadMoreItems: Boolean,

            @Assisted("usePageIndicators")
            usePageIndicators: Boolean = false,
        ): PostListEngine
    }

    companion object {
        private const val TAG = "PostListEngine"
    }

    private val coroutineScope = coroutineScopeFactory.create()

    var infinity: Boolean = infinity
        set(value) {
            if (value == field) return

            field = value

            if (!value && pages.isNotEmpty()) {
                val firstPage = pages.first()
                _pages = listOf(firstPage)
            }
            createItems()
        }

    var autoLoadMoreItems: Boolean = autoLoadMoreItems
        set(value) {
            if (value == field) return

            field = value

            createItems()
        }

    var usePageIndicators: Boolean = usePageIndicators
        set(value) {
            if (value == field) return

            field = value

            createItems()
        }

    val hasMore: Boolean
        get() = pages.lastOrNull()?.hasMore != false

    private var unfilteredItems = mutableSetOf<PostId>()

    private var _pages = listOf<LoadedPostsData>()

    /**
     * Sorted pages in ascending order.
     */
    val pages: List<LoadedPostsData>
        get() = _pages

    private var persistentErrors: List<Item> = listOf()
    private var expandedItems = mutableSetOf<String>()
    private var actionsExpandedItems = mutableSetOf<String>()
    private var postToHighlightForever: PostRef? = null
    private var postToHighlight: PostRef? = null

    private var _items: List<Item> = listOf()

    private var displayFirstItemsIndex: Int = -1
    private var displayLastItemsIndex: Int = -1

    private var key: String? = null
    private var secondaryKey: String? = null

    var isCommunityBlocked: Boolean = false

    val biggestPageIndex: Int?
        get() = pages.lastOrNull()?.pageIndex

    fun tryRestore() {
        Log.d(TAG, "Attempting to restore. Using keys $key, $secondaryKey")
        val cachedPages = directoryHelper.getPages(key, secondaryKey)

        cachedPages?.let {
            // We need to use let here because Google's lint rule doesn't support smart cast
            Log.d(
                TAG,
                "Restoration successful! Restored ${cachedPages.size} page(s) totalling " +
                    "${it.sumOf { it.posts.size }} posts.",
            )
            _pages = it
        }
    }

    fun setPersistentErrors(persistentErrors: List<Exception>) {
        this.persistentErrors = persistentErrors.map {
            Item.PersistentErrorItem(it)
        }
    }

    fun addPage(data: LoadedPostsData) {
        if (infinity) {
            val pages = pages.toMutableList()
            val pageIndexInPages = pages.indexOfFirst { it.dedupingKey == data.dedupingKey }
            if (pageIndexInPages != -1) {
                // replace!
                pages[pageIndexInPages] = data
            } else {
                pages.add(data)
            }
            pages.sortBy { it.pageIndex }
            _pages = pages
        } else {
            _pages = listOf(data)
        }

        coroutineScope.launch(Dispatchers.IO) {
            directoryHelper.addPage(key, secondaryKey, data, pages.size)
        }
    }

    fun createItems() {
        Log.d(TAG, "createItems()")

        if (pages.isEmpty()) {
            _items = listOf()
            return
        }

        val items = mutableListOf<Item>()

        val firstPage = pages.first()
        val lastPage = pages.last()

        items.add(Item.HeaderItem)

        if (isCommunityBlocked) {
            items.add(Item.PersistentErrorItem(CommunityBlockedError()))
        }

        items.addAll(persistentErrors)

        if (infinity && firstPage.pageIndex > 0) {
            items.add(Item.AutoLoadItem(firstPage.pageIndex - 1))
        }
        for (page in pages) {
            if (infinity && usePageIndicators) {
                items.add(Item.PageTitle(pageIndex = page.pageIndex))
            }

            if (page.error != null) {
                items.add(
                    Item.ErrorItem(
                        message = page.error.errorMessage,
                        pageToLoad = page.pageIndex,
                        isLoading = page.error.isLoading,
                    ),
                )
            } else {
                page.posts
                    .mapTo(items) {
                        val postView = it.fetchedPost.postView
                        val key = postView.getUniqueKey()
                        val isActionsExpanded = actionsExpandedItems.contains(key)
                        val isExpanded = expandedItems.contains(key)

                        if (it.filterReason != null && !unfilteredItems.contains(postView.post.id)) {
                            Item.FilteredPostItem(
                                fetchedPost = it.fetchedPost,
                                instance = page.instance,
                                isExpanded = isExpanded,
                                isActionExpanded = isActionsExpanded,
                                highlight = postToHighlight?.id == postView.post.id,
                                highlightForever = postToHighlightForever?.id == postView.post.id,
                                pageIndex = page.pageIndex,
                                filterReason = it.filterReason,
                            )
                        } else {
                            Item.VisiblePostItem(
                                fetchedPost = it.fetchedPost,
                                instance = page.instance,
                                isExpanded = isExpanded,
                                isActionExpanded = isActionsExpanded,
                                highlight = postToHighlight?.id == postView.post.id,
                                highlightForever = postToHighlightForever?.id == postView.post.id,
                                pageIndex = page.pageIndex,
                                isDuplicatePost = it.isDuplicatePost,
                            )
                        }
                    }
            }
        }
        if (infinity) {
            if (lastPage.error != null) {
                // add nothing!
            } else if (lastPage.hasMore) {
                if (autoLoadMoreItems) {
                    items.add(Item.AutoLoadItem(lastPage.pageIndex + 1))
                } else {
                    items.add(Item.ManualLoadItem(lastPage.pageIndex + 1))
                }
                items += Item.FooterSpacerItem
            } else {
                items.add(Item.EndItem)
                items += Item.FooterSpacerItem
            }
        } else {
            items.add(
                Item.FooterItem(
                    hasMore = lastPage.hasMore,
                    hasLess = lastPage.pageIndex != 0,
                ),
            )

            items += Item.FooterSpacerItem
        }

        _items = items
    }

    val items: List<Item>
        get() = _items

    fun clearHighlight() {
        postToHighlight = null
        postToHighlightForever = null
    }

    fun toggleItem(postView: PostView): Boolean {
        return if (expandedItems.contains(postView.getUniqueKey())) {
            expandedItems.remove(postView.getUniqueKey())

            false
        } else {
            expandedItems.add(postView.getUniqueKey())

            true
        }
    }

    fun toggleActions(postView: PostView) {
        if (actionsExpandedItems.contains(postView.getUniqueKey())) {
            actionsExpandedItems.remove(postView.getUniqueKey())
        } else {
            actionsExpandedItems.add(postView.getUniqueKey())
        }
    }

    fun highlight(postToHighlight: PostRef): Int {
        this.postToHighlight = postToHighlight
        this.postToHighlightForever = null

        return _items.indexOfFirst {
            when (it) {
                is Item.HeaderItem -> false
                is Item.FooterItem -> false
                is Item.AutoLoadItem -> false
                Item.EndItem -> false
                Item.FooterSpacerItem -> false
                is Item.ErrorItem -> false
                is Item.PostItem ->
                    it.fetchedPost.postView.post.id == postToHighlight.id
                is Item.PersistentErrorItem -> false
                is Item.ManualLoadItem -> false
                is Item.PageTitle -> false
            }
        }
    }

    fun highlightForever(postToHighlight: PostRef): Int {
        this.postToHighlight = null
        this.postToHighlightForever = postToHighlight
        return _items.indexOfFirst {
            when (it) {
                is Item.HeaderItem -> false
                is Item.FooterItem -> false
                is Item.AutoLoadItem -> false
                Item.EndItem -> false
                Item.FooterSpacerItem -> false
                is Item.ErrorItem -> false
                is Item.PostItem ->
                    it.fetchedPost.postView.post.id == postToHighlight.id
                is Item.PersistentErrorItem -> false
                is Item.ManualLoadItem -> false
                is Item.PageTitle -> false
            }
        }
    }

    fun endHighlightForever(): Int {
        val postToHighlight = postToHighlightForever

        return if (postToHighlight != null) {
            highlight(postToHighlight)
        } else {
            -1
        }
    }

    fun updateViewingPosition(firstItemsIndex: Int, lastItemsIndex: Int) {
        displayFirstItemsIndex = firstItemsIndex
        displayLastItemsIndex = lastItemsIndex
    }

    fun getCurrentPages(): List<Int> {
        if (displayFirstItemsIndex == -1 || displayLastItemsIndex == -1) {
            return listOf()
        }

        var firstPost: Item.VisiblePostItem? = null
        var lastPost: Item.VisiblePostItem? = null
        var i = displayFirstItemsIndex
        while (i < _items.size) {
            val item = _items.getOrNull(i)
            if (item is Item.VisiblePostItem) {
                firstPost = item
                break
            }
            i++
        }

        i = displayLastItemsIndex
        while (i > displayFirstItemsIndex) {
            val item = _items.getOrNull(i)
            if (item is Item.VisiblePostItem) {
                lastPost = item
                break
            }
            i--
        }

        if (firstPost == null || lastPost == null) {
            return listOf()
        }

        return (firstPost.pageIndex..lastPost.pageIndex).toList()
    }

    fun clear() {
        _pages = listOf()
        isCommunityBlocked = false
        coroutineScope.launch(Dispatchers.IO) {
            directoryHelper.clearPages(key, secondaryKey)
        }
    }

    fun getPostsCloseBy(): MutableList<FetchedPost> {
        val start = (displayFirstItemsIndex - 1).coerceAtLeast(0)
        val end = (start + 10).coerceAtMost(_items.size)

        val results = mutableListOf<FetchedPost>()

        for (i in start until end) {
            val item = _items[i]

            if (item is Item.VisiblePostItem) {
                results.add(item.fetchedPost)
            }
        }

        return results
    }

    fun getCommunityIcon(): String? =
        pages.firstOrNull()?.posts?.firstOrNull()?.fetchedPost?.postView?.community?.icon

    fun setKey(tag: String?) {
        key = tag
    }

    fun setSecondaryKey(key: String) {
        secondaryKey = key
    }

    fun updatePost(newPost: PostView) {
        val postId = newPost.post.id
        val pages = _pages.toMutableList()
        for ((index, page) in pages.withIndex()) {
            if (page.posts.any { it.fetchedPost.postView.post.id == postId }) {
                pages[index] = page.copy(
                    posts = page.posts.map {
                        if (it.fetchedPost.postView.post.id == postId) {
                            it.copy(
                                fetchedPost = it.fetchedPost.copy(
                                    postView = newPost,
                                ),
                            )
                        } else {
                            it
                        }
                    },
                )
            }
        }
        _pages = pages
    }

    fun markDuplicatePostsAsRead() {
        val pages = _pages.toMutableList()
        for ((index, page) in pages.withIndex()) {
            pages[index] = page.copy(
                posts = page.posts.map {
                    val postView = it.fetchedPost.postView
                    if (duplicatePostsDetector.isPostDuplicateOfRead(postView)) {
                        postReadManager.markPostAsReadLocal(
                            instance = postView.instance,
                            postId = postView.post.id,
                            read = true,
                        )
                        it.copy(
                            fetchedPost = it.fetchedPost.copy(
                                postView = postView.copy(
                                    read = true
                                )
                            ),
                            isDuplicatePost = true
                        )
                    } else {
                        it
                    }
                },
            )
        }
        _pages = pages
    }

    fun removePost(id: PostId) {
        val pages = _pages.toMutableList()
        for ((index, page) in pages.withIndex()) {
            if (page.posts.any { it.fetchedPost.postView.post.id == id }) {
                pages[index] = page.copy(
                    posts = page.posts.filter { it.fetchedPost.postView.post.id != id },
                )
            }
        }
        _pages = pages
    }

    fun setPageItemLoading(pageToLoad: Int) {
        _pages.find { it.pageIndex == pageToLoad }?.let {
            if (it.error != null && !it.error.isLoading) {
                it.error.isLoading = true

                createItems()
            }
        }
    }

    fun unfilter(postId: PostId) {
        unfilteredItems.add(postId)
    }
}
