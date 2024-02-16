package com.idunnololz.summit.lemmy.multicommunity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.info.AccountSubscription
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MultiCommunityEditorViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountInfoManager: AccountInfoManager,
) : ViewModel() {

    val showSearch = MutableLiveData<Boolean>()

    val searchResults = StatefulLiveData<List<CommunityView>>()
    val communityIcons = StatefulLiveData<List<String>>()
    val selectedCommunitiesFlow = MutableStateFlow<List<CommunityRef.CommunityRefByName>>(listOf())
    val selectedCommunitiesLiveData = selectedCommunitiesFlow.asLiveData()
    val communityName = MutableLiveData<String>()
    val selectedIcon = MutableLiveData<String?>()
    val subscribedCommunities = MutableLiveData<List<AccountSubscription>>()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            selectedCommunitiesFlow.collect {
                withContext(Dispatchers.Main) {
                    fetchCommunityIcons(it)
                }
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            accountInfoManager.currentFullAccount.collect {
                val subscriptions = it?.accountInfo?.subscriptions
                    ?: listOf()

                subscribedCommunities.postValue(subscriptions)
            }
        }
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
                    query = query,
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

    fun fetchCommunityIcons(communities: List<CommunityRef.CommunityRefByName>) {
        communityIcons.setIsLoading()

        viewModelScope.launch {
            val icons = flow {
                communities.forEach { community ->
                    apiClient
                        .fetchCommunityWithRetry(
                            Either.Right(community.getServerId(apiClient.instance)),
                            false,
                        )
                        .onSuccess {
                            val icon = it.community_view.community.icon
                            if (icon != null) {
                                emit(icon)
                            }
                        }
                }
            }.flowOn(Dispatchers.Default).toList()

            communityIcons.setValue(icons)
        }
    }

    fun setSelectedCommunities(communities: List<CommunityRef.CommunityRefByName>) {
        viewModelScope.launch {
            selectedCommunitiesFlow.emit(communities)
        }
    }
}
