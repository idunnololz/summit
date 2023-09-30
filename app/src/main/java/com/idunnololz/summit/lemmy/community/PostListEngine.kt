package com.idunnololz.summit.lemmy.community

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.offline.OfflineManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Item {
    data class PostItem(
        val postView: PostView,
        val instance: String,
        val isExpanded: Boolean,
        val isActionExpanded: Boolean,
        val highlight: Boolean,
        val highlightForever: Boolean,
        val pageIndex: Int,
    ) : Item()

    data class FooterItem(
        val hasMore: Boolean,
        val hasLess: Boolean,
    ) : Item()

    data class AutoLoadItem(val pageToLoad: Int) : Item()

    data class ErrorItem(val message: String, val pageToLoad: Int, val isLoading: Boolean) : Item()

    data class PersistentErrorItem(val exception: Exception) : Item()

    object EndItem : Item()
    object FooterSpacerItem : Item()
}

class PostListEngine(
    infinity: Boolean,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val offlineManager: OfflineManager,
) {

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
                _pages.value = listOf(firstPage)
            }
            createItems()
        }

    val hasMore: Boolean
        get() = pages.lastOrNull()?.hasMore != false

    private val _pages = MutableLiveData<List<LoadedPostsData>>()

    /**
     * Sorted pages in ascending order.
     */
    val pages: List<LoadedPostsData>
        get() = _pages.value ?: listOf()

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

    val biggestPageIndex: Int?
        get() = pages.lastOrNull()?.pageIndex

    fun tryRestore() {
        Log.d(TAG, "Attempting to restore. Using keys $key, $secondaryKey")
        val cachedPages = offlineManager.getPages(key, secondaryKey)

        cachedPages?.let {
            // We need to use let here because Google's lint rule doesn't support smart cast
            Log.d(TAG, "Restoration successful! Restored ${cachedPages.size} pages.")
            _pages.value = it
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
            val pageIndexInPages = pages.indexOfFirst { it.pageIndex == data.pageIndex }
            if (pageIndexInPages != -1) {
                // replace!
                pages[pageIndexInPages] = data
            } else {
                pages.add(data)
                pages.sortBy { it.pageIndex }
            }
            _pages.value = pages
        } else {
            _pages.value = listOf(data)
        }

        coroutineScope.launch(Dispatchers.IO) {
            offlineManager.addPage(key, secondaryKey, data, pages.size)
        }
    }

    fun createItems() {
        if (pages.isEmpty()) {
            _items = listOf()
            return
        }

        val items = mutableListOf<Item>()

        val firstPage = pages.first()
        val lastPage = pages.last()

        items.addAll(persistentErrors)

        if (infinity && firstPage.pageIndex > 0) {
            items.add(Item.AutoLoadItem(firstPage.pageIndex - 1))
        }
        for (page in pages) {
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
                        val key = it.getUniqueKey()
                        val isActionsExpanded = actionsExpandedItems.contains(key)
                        val isExpanded = expandedItems.contains(key)

                        Item.PostItem(
                            postView = it,
                            instance = page.instance,
                            isExpanded = isExpanded,
                            isActionExpanded = isActionsExpanded,
                            highlight = postToHighlight?.id == it.post.id,
                            highlightForever = postToHighlightForever?.id == it.post.id,
                            pageIndex = page.pageIndex,
                        )
                    }
            }
        }
        if (infinity) {
            if (lastPage.error != null) {
                // add nothing!
            } else if (lastPage.hasMore) {
                items.add(Item.AutoLoadItem(lastPage.pageIndex + 1))
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
                is Item.FooterItem -> false
                is Item.AutoLoadItem -> false
                Item.EndItem -> false
                Item.FooterSpacerItem -> false
                is Item.ErrorItem -> false
                is Item.PostItem ->
                    it.postView.post.id == postToHighlight.id
                is Item.PersistentErrorItem -> false
            }
        }
    }

    fun highlightForever(postToHighlight: PostRef): Int {
        this.postToHighlight = null
        this.postToHighlightForever = postToHighlight
        return _items.indexOfFirst {
            when (it) {
                is Item.FooterItem -> false
                is Item.AutoLoadItem -> false
                Item.EndItem -> false
                Item.FooterSpacerItem -> false
                is Item.ErrorItem -> false
                is Item.PostItem ->
                    it.postView.post.id == postToHighlight.id
                is Item.PersistentErrorItem -> false
            }
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

        var firstPost: Item.PostItem? = null
        var lastPost: Item.PostItem? = null
        var i = displayFirstItemsIndex
        while (i < _items.size) {
            val item = _items.getOrNull(i)
            if (item is Item.PostItem) {
                firstPost = item
                break
            }
            i++
        }

        i = displayLastItemsIndex
        while (i > displayFirstItemsIndex) {
            val item = _items.getOrNull(i)
            if (item is Item.PostItem) {
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

    fun clearPages() {
        _pages.value = listOf()
        coroutineScope.launch(Dispatchers.IO) {
            offlineManager.clearPages(key, secondaryKey)
        }
    }

    fun getPostsCloseBy(): MutableList<PostView> {
        val start = (displayFirstItemsIndex - 1).coerceAtLeast(0)
        val end = (start + 10).coerceAtMost(_items.size)

        val results = mutableListOf<PostView>()

        for (i in start until end) {
            val item = _items[i]

            if (item is Item.PostItem) {
                results.add(item.postView)
            }
        }

        return results
    }

    fun getCommunityIcon(): String? =
        pages.firstOrNull()?.posts?.firstOrNull()?.community?.icon

    fun setKey(tag: String?) {
        key = tag
    }

    fun setSecondaryKey(key: String) {
        secondaryKey = key
    }

    fun updatePost(newPost: PostView) {
        val postId = newPost.post.id
        val pages = _pages.value?.toMutableList() ?: return
        for ((index, page) in pages.withIndex()) {
            if (page.posts.any { it.post.id == postId }) {
                pages[index] = page.copy(
                    posts = page.posts.map {
                        if (it.post.id == postId) {
                            newPost
                        } else {
                            it
                        }
                    },
                )
            }
        }
        _pages.value = pages
    }

    fun removePost(id: PostId) {
        val pages = _pages.value?.toMutableList() ?: return
        for ((index, page) in pages.withIndex()) {
            if (page.posts.any { it.post.id == id }) {
                pages[index] = page.copy(posts = page.posts.filter { it.post.id != id })
            }
        }
        _pages.value = pages
    }

    fun setPageItemLoading(pageToLoad: Int) {
        _pages.value?.find { it.pageIndex == pageToLoad }?.let {
            if (it.error != null && !it.error.isLoading) {
                it.error.isLoading = true

                createItems()
            }
        }
    }
}
