package com.idunnololz.summit.lemmy.instancePicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.LemmyApiClient.Companion.DEFAULT_LEMMY_INSTANCES
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import info.debatty.java.stringsimilarity.NGram
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class InstancePickerViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {
    private val trigram = NGram(3)

    val searchResults = StatefulLiveData<SearchResults>()

    val instance
        get() = apiClient.instance

    init {
        doQuery("")
    }

    fun doQuery(query: String) {
        searchResults.setIsLoading()

        viewModelScope.launch {
            val results = DEFAULT_LEMMY_INSTANCES.filter { it.contains(query, ignoreCase = true) }
            val sortedResults = results.sortedBy {
                trigram.distance(
                    it,
                    query,
                )
            }

            searchResults.postValue(SearchResults(sortedResults))
        }
    }

    data class SearchResults(
        val results: List<String> = listOf(),
    )
}
