package com.idunnololz.summit.lemmy.communityPicker

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunityPickerViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {

    val searchResults = StatefulLiveData<List<CommunityView>>()
    val selectedCommunitiesFlow = MutableStateFlow<List<CommunityRef.CommunityRefByName>>(listOf())
    val communityName = MutableLiveData<String>()

    private var searchJob: Job? = null

    init {
    }

    fun doQuery(query: String) {
        searchResults.setIsLoading()

        if (query.isBlank()) {
            searchResults.setValue(listOf())
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            apiClient
                .search(
                    sortType = SortType.TopMonth,
                    listingType = ListingType.All,
                    searchType = SearchType.Communities,
                    query = query.toString(),
                    limit = 20,
                )
                .onSuccess {
                    searchResults.setValue(it.communities)
                }
                .onFailure {
                    searchResults.setError(it)
                }
        }
    }
}