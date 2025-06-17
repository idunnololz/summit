package com.idunnololz.summit.lemmy.inbox.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.PendingCommentsManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.CommentsFetcher
import com.idunnololz.summit.api.dto.Comment
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.filterLists.ContentFiltersManager
import com.idunnololz.summit.lemmy.CommentNodeData
import com.idunnololz.summit.lemmy.CommentTreeBuilder
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
