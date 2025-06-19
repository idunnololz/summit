package com.idunnololz.summit.lemmy.inbox.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.lemmy.inbox.message.ContextFetcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportDetailsViewModel @Inject constructor(
  private val apiClient: AccountAwareLemmyClient,
  private val contextFetcher: ContextFetcher,
  val accountManager: AccountManager,
) : ViewModel() {

  val apiInstance: String
    get() = apiClient.instance

  val commentContext = contextFetcher.commentContextFlow.asLiveData()

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
}