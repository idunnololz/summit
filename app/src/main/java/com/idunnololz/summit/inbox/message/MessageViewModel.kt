package com.idunnololz.summit.inbox.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MessageViewModel @Inject constructor(
  private val apiClient: AccountAwareLemmyClient,
  private val contextFetcher: ContextFetcher,
  val accountManager: AccountManager,
) : ViewModel() {

  val apiInstance: String
    get() = apiClient.instance

  val commentContext = contextFetcher.commentContextFlow.asLiveData()
  var isContextShowing = false

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
