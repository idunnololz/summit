package com.idunnololz.summit.lemmy.search

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.SearchType
import com.idunnololz.summit.api.dto.lemmy.SortType
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.SlidingPaneController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@HiltViewModel
class SearchTabbedViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val apiClient: AccountAwareLemmyClient,
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel(),
  SlidingPaneController.PostViewPagerViewModel {

  companion object {
    private const val MAX_QUERY_PAGE_LIMIT = 20

    private const val KEY_CURRENT_SORT_TYPE = "currentSortType"
  }

  val showSearch = MutableLiveData<Boolean>(true)
  val instance: String
    get() = apiClient.instance
  override var lastSelectedItem: Either<PostRef, CommentRef>? = null

  val queryEnginesByType = mutableMapOf<SearchType, QueryEngine>()

  private var currentType: SearchType = SearchType.All

  val currentQueryFlow = MutableStateFlow<String>("")
  val currentSortTypeFlow = savedStateHandle.getStateFlow(KEY_CURRENT_SORT_TYPE, SortType.Active)
  val currentPersonFilter = MutableStateFlow<PersonFilter?>(null)
  val currentCommunityFilter = MutableStateFlow<CommunityFilter?>(null)
  val nextPersonFilter = MutableLiveData<PersonFilter?>(null)
  val nextCommunityFilter = MutableLiveData<CommunityFilter?>(null)

  val currentQueryLiveData = currentQueryFlow.asLiveData()
  val currentPersonFilterLiveData = currentPersonFilter.asLiveData()
  val currentCommunityFilterLiveData = currentCommunityFilter.asLiveData()

  init {
    queryEnginesByType[SearchType.All] = QueryEngine(
      context = context,
      coroutineScopeFactory = coroutineScopeFactory,
      apiClient = apiClient,
      type = SearchType.All,
    )
    queryEnginesByType[SearchType.Url] = QueryEngine(
      context = context,
      coroutineScopeFactory = coroutineScopeFactory,
      apiClient = apiClient,
      type = SearchType.Url,
    )
    queryEnginesByType[SearchType.Posts] = QueryEngine(
      context = context,
      coroutineScopeFactory = coroutineScopeFactory,
      apiClient = apiClient,
      type = SearchType.Posts,
    )
    queryEnginesByType[SearchType.Comments] = QueryEngine(
      context = context,
      coroutineScopeFactory = coroutineScopeFactory,
      apiClient = apiClient,
      type = SearchType.Comments,
    )
    queryEnginesByType[SearchType.Communities] = QueryEngine(
      context = context,
      coroutineScopeFactory = coroutineScopeFactory,
      apiClient = apiClient,
      type = SearchType.Communities,
    )
    queryEnginesByType[SearchType.Users] = QueryEngine(
      context = context,
      coroutineScopeFactory = coroutineScopeFactory,
      apiClient = apiClient,
      type = SearchType.Users,
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
    viewModelScope.launch {
      currentPersonFilter.collect {
        queryEnginesByType[currentType]?.setPersonFilter(it?.personRef?.id)
      }
    }
    viewModelScope.launch {
      currentCommunityFilter.collect {
        queryEnginesByType[currentType]?.setCommunityFilter(it?.communityId)
      }
    }
  }

  fun setCurrentPersonFilter(personFilter: PersonFilter?) {
    viewModelScope.launch {
      currentPersonFilter.value = personFilter
    }
  }

  fun setCurrentCommunityFilter(communityFilter: CommunityFilter?) {
    viewModelScope.launch {
      currentCommunityFilter.value = communityFilter
    }
  }

  fun updateCurrentQuery(query: String) {
    viewModelScope.launch {
      currentQueryFlow.value = query
    }
  }

  fun setActiveType(type: SearchType) {
    currentType = type

    queryEnginesByType[currentType]?.apply {
      setSortType(currentSortTypeFlow.value)
      setPersonFilter(currentPersonFilter.value?.personRef?.id)
      setCommunityFilter(currentCommunityFilter.value?.communityId)

      // Set query must be last to avoid race conditions
      setQuery(currentQueryFlow.value)
    }
  }

  fun setSortType(type: SortType) {
    viewModelScope.launch {
      savedStateHandle[KEY_CURRENT_SORT_TYPE] = type
    }
  }

  fun loadPage(pageIndex: Int, force: Boolean = false) {
    queryEnginesByType[currentType]?.performQuery(pageIndex, force)
  }

  @Parcelize
  data class PersonFilter(
    val personRef: PersonRef.PersonRefComplete,
  ) : Parcelable

  @Parcelize
  data class CommunityFilter(
    val communityId: Int,
    val communityRef: CommunityRef.CommunityRefByName,
  ) : Parcelable
}
