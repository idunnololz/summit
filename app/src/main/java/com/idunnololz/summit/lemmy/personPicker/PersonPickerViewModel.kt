package com.idunnololz.summit.lemmy.personPicker

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class PersonPickerViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {

    data class PersonSearchResults(
        val people: List<PersonView>,
        val query: String,
    )

    val instance: String
        get() = apiClient.instance
    val searchResults = StatefulLiveData<PersonSearchResults>()
    val personName = MutableLiveData<String>()

    private var searchJob: Job? = null

    fun doQuery(query: String) {
        searchResults.setIsLoading()

        if (query.isBlank()) {
            searchResults.setValue(PersonSearchResults(listOf(), query))
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            apiClient
                .search(
                    sortType = SortType.Active,
                    listingType = ListingType.All,
                    searchType = SearchType.Users,
                    query = query,
                    limit = 20,
                )
                .onSuccess {
                    searchResults.setValue(
                        PersonSearchResults(it.users, query))
                }
                .onFailure {
                    searchResults.setError(it)
                }
        }
    }
}
