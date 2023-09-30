package com.idunnololz.summit.lemmy.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportContentViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {

    val reportState = StatefulLiveData<Unit>()

    var sendReportJob: Job? = null

    fun sendReport(postOrCommentRef: Either<PostRef, CommentRef>, reason: String) {
        reportState.setIsLoading()

        sendReportJob?.cancel()
        sendReportJob = viewModelScope.launch {
            postOrCommentRef
                .fold(
                    ifLeft = {
                        apiClient.createPostReport(it.id, reason)
                    },
                    ifRight = {
                        apiClient.createCommentReport(it.id, reason)
                    },
                )
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
