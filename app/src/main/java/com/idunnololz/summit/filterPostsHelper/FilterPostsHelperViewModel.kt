package com.idunnololz.summit.filterPostsHelper

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.idunnololz.summit.filterLists.ContentFiltersManager
import com.idunnololz.summit.filterLists.ContentTypeIds
import com.idunnololz.summit.filterLists.FilterEntry
import com.idunnololz.summit.filterLists.FilterTypeIds
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FilterPostsHelperViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  private val contentFiltersManager: ContentFiltersManager,
) : ViewModel() {

  suspend fun addContentFilter(filters: List<String>) {
    filters.forEach {
      contentFiltersManager.addFilter(
        FilterEntry(
          id = 0,
          contentType = ContentTypeIds.PostListType,
          filterType = FilterTypeIds.KeywordFilter,
          filter = it,
          isRegex = false,
          options = null,
        )
      )
    }
  }
}