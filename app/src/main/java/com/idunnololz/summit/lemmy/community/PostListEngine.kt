package com.idunnololz.summit.lemmy.community

import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.lemmy.PostRef


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

    data class FooterItem(val hasMore: Boolean) : Item()

    data class AutoLoadItem(val pageToLoad: Int) : Item()

    data class ErrorItem(val error: Throwable, val pageToLoad: Int) : Item()

    object EndItem : Item()
}

class PostListEngine(
    infinity: Boolean
) {

    var infinity: Boolean = infinity
        set(value) {
            if (value == field) return

            field = value

            if (!value && pages.size > 0) {
                val firstPage = pages.first()
                pages.clear()
                pages.add(firstPage)
            }
            createItems()
        }

    /**
     * Sorted pages in ascending order.
     */
    val pages = mutableListOf<CommunityViewModel.LoadedPostsData>()

    private var expandedItems = mutableSetOf<String>()
    private var actionsExpandedItems = mutableSetOf<String>()
    private var postToHighlightForever: PostRef? = null
    private var postToHighlight: PostRef? = null

    private var _items: List<Item> = listOf()

    private var displayFirstItemsIndex: Int = -1
    private var displayLastItemsIndex: Int = -1

    val biggestPageIndex: Int?
        get() = pages.lastOrNull()?.pageIndex

    fun addPage(data: CommunityViewModel.LoadedPostsData) {
        if (infinity) {
            val pageIndexInPages = pages.indexOfFirst { it.pageIndex == data.pageIndex }
            if (pageIndexInPages != -1) {
                // replace!
                pages[pageIndexInPages] = data
            } else {
                pages.add(data)
                pages.sortBy { it.pageIndex }
            }
        } else {
            pages.clear()
            pages.add(data)
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

        if (infinity && firstPage.pageIndex > 0) {
            items.add(Item.AutoLoadItem(firstPage.pageIndex - 1))
        }
        for (page in pages) {
            if (page.error != null) {
                items.add(Item.ErrorItem(page.error, page.pageIndex))
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
            } else {
                items.add(Item.EndItem)
            }
        } else {
            items.add(Item.FooterItem(lastPage.hasMore))
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
        return  _items.indexOfFirst {
            when (it) {
                is Item.FooterItem -> false
                is Item.AutoLoadItem -> false
                Item.EndItem -> false
                is Item.ErrorItem -> false
                is Item.PostItem ->
                    it.postView.post.id == postToHighlight.id
            }
        }
    }

    fun highlightForever(postToHighlight: PostRef): Int {
        this.postToHighlight = null
        this.postToHighlightForever = postToHighlight
        return  _items.indexOfFirst {
            when (it) {
                is Item.FooterItem -> false
                is Item.AutoLoadItem -> false
                Item.EndItem -> false
                is Item.ErrorItem -> false
                is Item.PostItem ->
                    it.postView.post.id == postToHighlight.id
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
            val item = _items[i]
            if (item is Item.PostItem) {
                firstPost = item
                break
            }
            i++
        }

        i = displayLastItemsIndex
        while (i > displayFirstItemsIndex) {
            val item = _items[i]
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

    fun getAllPageIndices(): List<Int> =
        pages.map { it.pageIndex }

    fun clearPages() {
        pages.clear()
    }

    fun getPostsCloseBy(): MutableList<PostView> {
        val start = (displayFirstItemsIndex - 1).coerceAtLeast(0)
        val end = (start + 20).coerceAtMost(_items.size)

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
}