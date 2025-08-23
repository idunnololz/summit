package com.idunnololz.summit.uploads

import android.content.Context
import android.util.Log
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.LocalImageView
import com.idunnololz.summit.lemmy.utils.listSource.LemmyListSource
import com.idunnololz.summit.lemmy.utils.listSource.MultiLemmyListSource
import com.idunnololz.summit.lemmy.utils.listSource.PageResult
import com.idunnololz.summit.util.dateStringToTs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class UploadsEngine @Inject constructor(
  @ApplicationContext private val context: Context,
  private val apiClient: AccountAwareLemmyClient,
) {

  sealed interface UploadItem {
    data class MediaItem(
      val media: LocalImageView,
      val instance: String,
    ) : UploadItem

    data class LoadingItem(
      val pageIndex: Int,
    ) : UploadItem

    data class MoreItem(
      val pageIndex: Int,
    ) : UploadItem

    data class ErrorItem(
      val error: Throwable,
      val pageIndex: Int,
    ) : UploadItem
  }

  private val source =
    MultiLemmyListSource(
      listOf(
        LemmyListSource<LocalImageView, Unit, String>(
          context = context,
          id = { this.local_image.pictrs_alias },
          defaultSortOrder = Unit,
          fetchObjects = {
              pageIndex: Int,
              sortOrder: Unit,
              limit: Int,
              force: Boolean,
            ->
            apiClient.listMedia(
              page = pageIndex.toLong(),
              limit = limit.toLong(),
              force = force,
            ).map { it.images }
          },
        ),
      ),
      sortValue = { dateStringToTs(it.local_image.published) },
      id = { it.local_image.pictrs_alias },
    )

  val itemsFlow = MutableStateFlow<List<UploadItem>>(listOf())

  private val loadingPages = mutableSetOf<Int>()
  private val pages = mutableListOf<PageResult<LocalImageView>>()

  suspend fun fetchPage(pageIndex: Int, force: Boolean) {
    withContext(Dispatchers.Main) {
      loadingPages.add(pageIndex)
      publishItems()
    }

    val result = source.getPage(pageIndex, force = force, retainItemsOnForce = true)

    if (pages.size <= pageIndex) {
      pages.add(result)
    } else {
      pages[pageIndex] = result
    }

    withContext(Dispatchers.Main) {
      loadingPages.remove(pageIndex)
      publishItems()
    }
  }

  private fun publishItems() {
    val newItems = mutableListOf<UploadItem>()
    for (p in pages) {
      when (p) {
        is PageResult.ErrorPageResult -> {
          newItems.add(UploadItem.ErrorItem(p.error, p.pageIndex))
        }

        is PageResult.SuccessPageResult -> {
          newItems.addAll(
            p.items.map {
              UploadItem.MediaItem(
                media = it,
                instance = apiClient.instance,
              )
            },
          )
        }
      }
    }

    val lastPage = pages.lastOrNull()
    val lastPageLoaded = lastPage?.pageIndex ?: -1
    if (loadingPages.contains(lastPageLoaded + 1)) {
      newItems.add(UploadItem.LoadingItem(lastPageLoaded + 1))
    } else if (lastPage?.getOrNull()?.hasMore == true) {
      newItems.add(UploadItem.MoreItem(lastPageLoaded + 1))
    }

    itemsFlow.value = newItems
  }

  fun reset() {
    source.invalidate()
    loadingPages.clear()
    pages.clear()
  }

  fun removeItem(filename: String) {
    for ((index, page) in pages.withIndex()) {
      if (page is PageResult.SuccessPageResult) {
        if (page.items.any { it.local_image.pictrs_alias == filename }) {
          pages[index] =
            page.copy(items = page.items.filter { it.local_image.pictrs_alias != filename })
        }
      }
    }
    publishItems()
  }
}
