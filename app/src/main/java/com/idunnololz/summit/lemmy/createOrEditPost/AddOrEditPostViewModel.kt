package com.idunnololz.summit.lemmy.createOrEditPost

import android.webkit.URLUtil
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.Language
import com.idunnololz.summit.api.dto.LanguageId
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftsManager
import com.idunnololz.summit.lemmy.RecentCommunityManager
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.links.LinkMetadataHelper
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class) // Flow.debounce()
@HiltViewModel
class AddOrEditPostViewModel @Inject constructor(
  private val apiClient: AccountAwareLemmyClient,
  private val accountManager: AccountManager,
  private val state: SavedStateHandle,
  private val linkMetadataHelper: LinkMetadataHelper,
  val draftsManager: DraftsManager,
  private val recentCommunityManager: RecentCommunityManager,
) : ViewModel() {

  companion object {
    private const val TAG = "CreateOrEditPostViewModel"
  }

  var postPrefilled: Boolean = false
  val createOrEditPostResult = StatefulLiveData<PostView>()
  val searchResults = StatefulLiveData<List<CommunityView>>()
  val showSearch = MutableStateFlow<Boolean>(false)
  val showSearchLiveData = showSearch.asLiveData()
  val query = MutableStateFlow("")
  val linkMetadata = StatefulLiveData<LinkMetadataHelper.LinkMetadata>()
  val showMore = state.getLiveData<Boolean>("show_more")

  val currentDraftEntry = state.getLiveData<DraftEntry>("current_draft_entry")
  val currentDraftId = state.getLiveData<Long>("current_draft_id")
  val languageOptions = state.getLiveData<List<Language>>("languages")
  val languageId = state.getLiveData<LanguageId?>("language")

  val currentAccount: Account?
    get() = accountManager.currentAccount.asAccount

  private var searchJob: Job? = null

  private var urlFlow = MutableSharedFlow<String>()

  init {
    viewModelScope.launch {
      query
        .debounce(300)
        .collect {
          doQuery(it)
        }
    }
    viewModelScope.launch {
      urlFlow
        .debounce(500)
        .collect {
          if (LinkUtils.isValidUrl(it)) {
            loadLinkMetadata(it)
          }
        }
    }
    viewModelScope.launch {
      apiClient.fetchSiteWithRetry(force = false)
        .onSuccess {
          languageOptions.value = it.all_languages
        }
    }
  }

  fun createPost(
    communityFullName: String,
    name: String,
    body: String,
    url: String,
    thumbnailUrl: String?,
    altText: String?,
    isNsfw: Boolean,
  ) {
    if (createOrEditPostResult.isLoading) {
      return
    }

    if (name.isBlank()) {
      createOrEditPostResult.postError(NoTitleError())
      return
    }

    createOrEditPostResult.setIsLoading()
    viewModelScope.launch {
      val communityResult =
        apiClient.fetchCommunityWithRetry(
          Either.Right(communityFullName),
          force = false,
        )
          .fold(
            {
              Result.success(it.community_view.community)
            },
            {
              Result.failure(it)
            },
          )

      if (communityResult.isFailure) {
        createOrEditPostResult.postError(
          requireNotNull(communityResult.exceptionOrNull()),
        )
        return@launch
      }

      val community = communityResult.getOrThrow()

      val result = apiClient.createPost(
        name = name,
        body = body.ifBlank { null },
        url = url.ifBlank { null },
        isNsfw = isNsfw,
        communityId = community.id,
        thumbnailUrl = thumbnailUrl,
        altText = altText,
        languageId = languageId.value,
      )

      recentCommunityManager.addRecentCommunityPostedTo(
        community.toCommunityRef(), community.icon)

      result
        .onSuccess {
          createOrEditPostResult.postValue(it)
        }
        .onFailure {
          createOrEditPostResult.postError(it)
        }
    }
  }

  fun updatePost(
    instance: String,
    name: String,
    body: String,
    url: String,
    thumbnailUrl: String?,
    altText: String?,
    isNsfw: Boolean,
    postId: PostId,
  ) {
    createOrEditPostResult.setIsLoading()
    viewModelScope.launch {
      val account = accountManager.currentAccount.asAccount

      if (account == null) {
        createOrEditPostResult.postError(NotAuthenticatedException())
        return@launch
      }

      if (name.isBlank()) {
        createOrEditPostResult.postError(NoTitleError())
        return@launch
      }

      val result = apiClient.editPost(
        postId = postId,
        name = name,
        body = body.ifBlank { null },
        url = url.ifBlank { null },
        isNsfw = isNsfw,
        account = account,
        thumbnailUrl = thumbnailUrl,
        altText = altText,
        languageId = languageId.value,
      )

      result
        .onSuccess {
          createOrEditPostResult.postValue(it)
        }
        .onFailure {
          createOrEditPostResult.postError(it)
        }
    }
  }

  fun loadLinkMetadata(url: String) {
    linkMetadata.setIsLoading()

    viewModelScope.launch {
      try {
        linkMetadata.setValue(linkMetadataHelper.loadLinkMetadata(url))
      } catch (e: Exception) {
        linkMetadata.setError(e)
      }
    }
  }

  fun setUrl(url: String) {
    viewModelScope.launch {
      urlFlow.emit(url)
    }
  }

  private fun doQuery(query: String) {
    searchResults.setIsLoading()

    if (query.isBlank()) {
      searchResults.setValue(listOf())
      return
    }

    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      apiClient
        .searchWithRetry(
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

  class NoTitleError : RuntimeException()
}
