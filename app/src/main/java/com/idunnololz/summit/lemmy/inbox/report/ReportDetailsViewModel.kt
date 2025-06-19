package com.idunnololz.summit.lemmy.inbox.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.ReportItem
import com.idunnololz.summit.lemmy.inbox.message.ContextFetcher
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportDetailsViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  private val apiClient: AccountAwareLemmyClient,
  private val contextFetcher: ContextFetcher,
  val accountManager: AccountManager,
) : ViewModel() {

  val apiInstance: String
    get() = apiClient.instance

  var isResolved = savedStateHandle.getLiveData<Boolean>("is_resolved", false)

  val commentContext = contextFetcher.commentContextFlow.asLiveData()
  val reportedPersonInfo = StatefulLiveData<GetPersonDetailsResponse>()

  init {
    addCloseable(contextFetcher)
  }

  fun fetchCommentContext(postId: Int, commentPath: String?, force: Boolean) {
    viewModelScope.launch {
      contextFetcher
        .fetchCommentContext(
          postId = postId,
          commentPath = commentPath,
          force = force,
        )
    }
  }

  fun fetchReportedPersonInfo(personId: PersonId) {
    reportedPersonInfo.setIsLoading()

    viewModelScope.launch {
      apiClient.fetchPersonByIdWithRetry(personId, force = false)
        .onSuccess {
          reportedPersonInfo.postValue(it)
        }
        .onFailure {
          reportedPersonInfo.postError(it)
        }
    }
  }
}