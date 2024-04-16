package com.idunnololz.summit.lemmy.communities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.toLemmyPageIndex
import com.idunnololz.summit.lemmy.utils.ListEngine
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CommunitiesViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val noAuthApiClient: LemmyApiClient,
) : ViewModel() {

    companion object {
        private const val PAGE_ENTRIES_LIMIT = 50
    }

    val apiInstance: String
        get() = apiClient.instance
    private val communitiesEngine = ListEngine<CommunityView>()

    val communitiesData = StatefulLiveData<CommunitiesData>()

    init {
        viewModelScope.launch {
            communitiesEngine.items.collect {
                communitiesData.postValue(CommunitiesData(it))
            }
        }
    }

    fun fetchCommunities(page: Int) {
        communitiesData.setIsLoading()

        viewModelScope.launch {
            val result = noAuthApiClient
                .fetchCommunities(
                    sortType = SortType.TopAll,
                    listingType = ListingType.Local,
                    page = page.toLemmyPageIndex(),
                    limit = PAGE_ENTRIES_LIMIT,
                    account = null,
                )
            communitiesEngine.addPage(
                page = page,
                communities = result,
                hasMore = result.fold(
                    onSuccess = {
                        it.size == PAGE_ENTRIES_LIMIT
                    },
                    onFailure = {
                        true
                    },
                ),
            )
        }
    }

    fun setCommunitiesInstance(instance: String) {
        noAuthApiClient.changeInstance(instance)
    }

    fun reset() {
        viewModelScope.launch {
            communitiesEngine.clear()
            fetchCommunities(0)
        }
    }

    data class CommunitiesData(
        val data: List<ListEngine.Item<CommunityView>>,
    )
}
