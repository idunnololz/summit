package com.idunnololz.summit.lemmy.utils

import kotlinx.coroutines.flow.MutableSharedFlow

class ListEngine<T> {
    private val pages = mutableMapOf<Int, Page<T>>()

    val items = MutableSharedFlow<List<Item<T>>>()

    suspend fun addPage(page: Int, communities: Result<List<T>>, hasMore: Boolean) {
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
        val items = mutableListOf<Item<T>>()

        val pages = pages.values.sortedBy { it.pageIndex }

        if (pages.isEmpty() || pages.all {
                it.data.fold(
                    onSuccess = { it.items.isEmpty() },
                    onFailure = { false },
                )
            }
        ) {
            @Suppress("UNCHECKED_CAST")
            items.add(Item.EmptyItem())
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
                    for (item in it.items) {
                        items.add(Item.DataItem(item))
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

    sealed interface Item<T> {
        data class DataItem<T>(
            private val _data: T?,
        ) : Item<T> {
            val data: T
                get() = _data!!
        }

        class EmptyItem<T>() : Item<T>

        data class LoadItem<T>(
            val pageIndex: Int = 0,
        ) : Item<T>

        data class ErrorItem<T>(
            val pageIndex: Int,
            val error: Throwable,
        ) : Item<T> {
            constructor() : this(0, RuntimeException())
        }
    }

    data class Page<T>(
        val pageIndex: Int,
        val data: Result<Data<T>>,
        val hasMore: Boolean,
    )

    data class Data<T>(
        val items: List<T>,
    )
}
