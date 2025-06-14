package com.idunnololz.summit.lemmy.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.inbox.conversation.MessageItem
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class ReportContentViewModel @Inject constructor(
  private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {

  val reportState = StatefulLiveData<Unit>()

  var sendReportJob: Job? = null

  fun sendReport(
    postRef: PostRef?,
    commentRef: CommentRef?,
    messageItem: MessageItem?,
    reason: String
  ) {
    reportState.setIsLoading()

    sendReportJob?.cancel()
    sendReportJob = viewModelScope.launch {
      if (postRef != null) {
        apiClient.createPostReport(postRef.id, reason)
      } else if (commentRef != null) {
        apiClient.createCommentReport(commentRef.id, reason)
      } else {
        apiClient.createPrivateMessageReport(requireNotNull(messageItem).id, reason)
      }
        .onSuccess {
          reportState.postValue(Unit)
        }
        .onFailure {
          reportState.postError(it)
        }
    }
  }

  fun cancelSendReport() {
    sendReportJob?.cancel()
    reportState.setIdle()
  }
}
