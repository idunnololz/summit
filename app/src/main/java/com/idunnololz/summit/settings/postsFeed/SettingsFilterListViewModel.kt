package com.idunnololz.summit.settings.postsFeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.filterLists.ContentFiltersManager
import com.idunnololz.summit.filterLists.ContentTypeId
import com.idunnololz.summit.filterLists.FilterEntry
import com.idunnololz.summit.filterLists.FilterTypeId
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsFilterListViewModel @Inject constructor(
    private val contentFiltersManager: ContentFiltersManager,
) : ViewModel() {

    val filters = StatefulLiveData<List<FilterEntry>>()

    private var contentTypeId: ContentTypeId? = null
    private var filterTypeId: FilterTypeId? = null

    fun getFilters(contentTypeId: ContentTypeId, filterTypeId: FilterTypeId) {
        this.contentTypeId = contentTypeId
        this.filterTypeId = filterTypeId

        refreshFilters()
    }

    fun addFilter(result: FilterEntry) {
        filters.setIsLoading()

        viewModelScope.launch {
            contentFiltersManager.addFilter(result)

            refreshFilters()
        }
    }

    fun deleteFilter(filter: FilterEntry) {
        viewModelScope.launch {
            contentFiltersManager.deleteFilter(filter)

            refreshFilters()
        }
    }

    private fun refreshFilters() {
        val contentTypeId = contentTypeId ?: return
        val filterTypeId = filterTypeId ?: return

        filters.setIsLoading()

        viewModelScope.launch {
            val filters = contentFiltersManager.getFilters(contentTypeId, filterTypeId)

            this@SettingsFilterListViewModel.filters.postValue(filters)
        }
    }
}
