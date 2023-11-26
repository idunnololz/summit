package com.idunnololz.summit.lemmy.communities

import com.idunnololz.summit.api.dto.CommunityView
import kotlinx.coroutines.flow.MutableStateFlow

class CommunitiesEngine {

    private val pages = mutableMapOf<Int, Page>()

    val items = MutableStateFlow<List<Item>>(listOf())

    suspend fun addPage(page: Int, communities: Result<List<CommunityView>>, hasMore: Boolean) {
        pages[page] = Page(
            pageIndex = page,
            data = communities.fold(
                {
                    Result.success(Data(it))
                },
                {
                    Result.failure(it)
                },
            ),
            hasMore = hasMore,
        )

        buildItems()
    }

    private suspend fun buildItems() {
        val items = mutableListOf<Item>()

        val pages = pages.values.sortedBy { it.pageIndex }

        if (pages.isEmpty()) {
            items.add(Item.EmptyItem)
            this.items.emit(items)
            return
        }

        val startIndex = 0
        val lastPage = pages.last()
        val endIndex = lastPage.pageIndex
        val hasMore = lastPage.hasMore

        for (index in startIndex..endIndex) {
            val page = pages.getOrNull(index)

            if (page == null) {
                items.add(Item.LoadItem(index))
                continue
            }

            page.data
                .onSuccess {
                    for (community in it.communities) {
                        items.add(Item.CommunityItem(community))
                    }
                }
                .onFailure {
                    items.add(Item.ErrorItem(index, it))
                }
        }

        if (hasMore) {
            items.add(Item.LoadItem(endIndex + 1))
        }

        this.items.emit(items)
    }

    suspend fun clear() {
        this.pages.clear()
        this.items.emit(listOf())
    }

    sealed interface Item {
        data class CommunityItem(
            val community: CommunityView,
        ) : Item

        data object EmptyItem : Item

        data class LoadItem(
            val pageIndex: Int,
        ) : Item

        data class ErrorItem(
            val pageIndex: Int,
            val error: Throwable,
        ) : Item
    }

    data class Page(
        val pageIndex: Int,
        val data: Result<Data>,
        val hasMore: Boolean,
    )

    data class Data(
        val communities: List<CommunityView>,
    )
}
