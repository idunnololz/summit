package com.idunnololz.summit.lemmy.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.ViewPagerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) : ViewModel(), ViewPagerController.PostViewPagerViewModel {

    companion object {
         private const val MAX_QUERY_PAGE_LIMIT = 20
    }

    val instance: String
        get() = apiClient.instance
    override var lastSelectedPost: PostRef? = null
    override val viewPagerAdapter = ViewPagerController.ViewPagerAdapter()

    val queryEnginesByType = mutableMapOf<SearchType, QueryEngine>()

    private var currentType: SearchType = SearchType.All

    val currentQueryFlow = MutableStateFlow<String>("")
    val currentQueryLiveData = currentQueryFlow.asLiveData()
    val currentSortTypeFlow = MutableStateFlow<SortType>(SortType.Active)

    init {
        queryEnginesByType[SearchType.All] = QueryEngine(
            coroutineScopeFactory,
            apiClient,
            SearchType.All,
        )
        queryEnginesByType[SearchType.Url] = QueryEngine(
            coroutineScopeFactory,
            apiClient,
            SearchType.Url,
        )
        queryEnginesByType[SearchType.Posts] = QueryEngine(
            coroutineScopeFactory,
            apiClient,
            SearchType.Posts,
        )
        queryEnginesByType[SearchType.Comments] = QueryEngine(
            coroutineScopeFactory,
            apiClient,
            SearchType.Comments,
        )
        queryEnginesByType[SearchType.Communities] = QueryEngine(
            coroutineScopeFactory,
            apiClient,
            SearchType.Communities,
        )
        queryEnginesByType[SearchType.Users] = QueryEngine(
            coroutineScopeFactory,
            apiClient,
            SearchType.Users,
        )

        viewModelScope.launch {
            currentQueryFlow.collect {
                queryEnginesByType[currentType]?.setQuery(it)
            }
        }
        viewModelScope.launch {
            currentSortTypeFlow.collect {
                queryEnginesByType[currentType]?.setSortType(it)
            }
        }
    }

    fun updateCurrentQuery(query: String) {
        viewModelScope.launch {
            currentQueryFlow.value = query
        }
    }

    fun setActiveType(type: SearchType) {
        currentType = type

        queryEnginesByType[currentType]?.setQuery(currentQueryFlow.value)
        queryEnginesByType[currentType]?.setSortType(currentSortTypeFlow.value)
    }

    fun setSortType(type: SortType) {
        viewModelScope.launch {
            currentSortTypeFlow.value = type
        }
    }

    fun loadPage(pageIndex: Int, force: Boolean = false) {
        queryEnginesByType[currentType]?.performQuery(pageIndex, force)
    }
}