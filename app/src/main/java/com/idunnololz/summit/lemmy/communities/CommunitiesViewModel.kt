package com.idunnololz.summit.lemmy.communities

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.ApiFeature
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.lemmy.CommunityView
import com.idunnololz.summit.api.dto.lemmy.ListingType
import com.idunnololz.summit.api.dto.lemmy.SortType
import com.idunnololz.summit.api.toLemmyPageIndex
import com.idunnololz.summit.lemmy.utils.ListEngine
import com.idunnololz.summit.lemmy.utils.listSource.CursorBackedSingleDataSource
import com.idunnololz.summit.lemmy.utils.listSource.LemmyListSource
import com.idunnololz.summit.lemmy.utils.listSource.Page
import com.idunnololz.summit.lemmy.utils.listSource.SimpleDataSource
import com.idunnololz.summit.lemmy.utils.listSource.fold
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CommunitiesViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val apiClient: AccountAwareLemmyClient,
  private val noAuthApiClient: LemmyApiClient,
) : ViewModel() {

  class CommunitiesSource(
    context: Context,
    simpleDataSource: SimpleDataSource<CommunityView, SortType>,
  ) : LemmyListSource<CommunityView, SortType, Long>(
    context,
    { this.community.id.toLong() },
    SortType.New,
    {
        pageIndex: Int,
        sortOrder: SortType,
        limit: Int,
        force: Boolean,
      ->

      simpleDataSource.fetchItems(sortOrder, pageIndex, force, limit = limit, showRead = true)
    },
  )

  class PagedCommunitiesSource(
    private val noAuthApiClient: LemmyApiClient,
  ) : SimpleDataSource<CommunityView, SortType> {
    override suspend fun fetchItems(
      sortOrder: SortType?,
      page: Int,
      force: Boolean,
      limit: Int,
      showRead: Boolean?,
    ): Result<List<CommunityView>> = noAuthApiClient
      .fetchCommunities(
        sortType = SortType.TopAll,
        listingType = ListingType.Local,
        page = page.toLemmyPageIndex(),
        limit = limit,
        account = null,
      )
      .map {
        it.communities
      }
  }

  class CursorCommunitiesSource(
    private val noAuthApiClient: LemmyApiClient,
  ) : CursorBackedSingleDataSource<CommunityView, SortType>(
    fetchObjects = {
        pageCursor: String?,
        sortOrder: SortType?,
        limit: Int,
        force: Boolean,
        showRead: Boolean?,
      ->

      noAuthApiClient
        .fetchCommunities(
          sortType = SortType.TopAll,
          listingType = ListingType.Local,
          pageCursor = pageCursor,
          limit = limit,
          account = null,
        )
        .map {
          Page(
            it.communities,
            it.next_page,
          )
        }
    },
  )

  companion object {
    private const val PAGE_ENTRIES_LIMIT = 50
  }

  private var communitiesSource: CommunitiesSource? = null

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
      if (communitiesSource == null) {
        communitiesSource = CommunitiesSource(
          context,
          if (noAuthApiClient.supportsFeature(ApiFeature.ListByCursorRequired).getOrNull() ==
            true
          ) {
            CursorCommunitiesSource(noAuthApiClient)
          } else {
            PagedCommunitiesSource(noAuthApiClient)
          },
        )
      }

      val result = communitiesSource?.getPage(page.toLemmyPageIndex(), force = false)

      communitiesEngine.addPage(
        page = page,
        communities = result?.fold(
          { Result.success(it.items) },
          { Result.failure(it) },
        ) ?: Result.failure(RuntimeException("Community source is null!")),
        hasMore = result?.fold(
          onSuccess = {
            it.hasMore
          },
          onFailure = {
            true
          },
        ) ?: true,
      )
    }
  }

  fun setCommunitiesInstance(instance: String) {
    noAuthApiClient.changeInstance(instance)
    communitiesSource = null
  }

  fun reset() {
    viewModelScope.launch {
      communitiesEngine.clear()
      fetchCommunities(0)
      communitiesSource = null
    }
  }

  data class CommunitiesData(
    val data: List<ListEngine.Item<CommunityView>>,
  )
}
