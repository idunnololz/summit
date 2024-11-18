package com.idunnololz.summit.offline.dialog

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.offline.OfflinePostFeedWork
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MakeOfflineViewModel @Inject constructor(
    @Suppress("StaticFieldLeak")
    @ApplicationContext private val context: Context,
    private val state: SavedStateHandle,
) : ViewModel() {

    private val didRunWorker = state.getLiveData<Boolean>("did_run_worker")
    private val workRequestId = state.getLiveData<UUID>("work_request_id")

    val progress = MutableLiveData<OfflinePostFeedWork.ProgressTracker>()

    init {
        workRequestId.observeForever { requestId ->
            val workManager = WorkManager.getInstance(context)

            workManager.getWorkInfoByIdLiveData(requestId)
                .observeForever { workInfo ->
                    if (workInfo != null) {
                        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                            progress.postValue(
                                OfflinePostFeedWork.ProgressTracker().apply {
                                    this.currentPhase = OfflinePostFeedWork.ProgressPhase.Complete
                                },
                            )
                        } else {
                            progress.postValue(
                                OfflinePostFeedWork.ProgressTracker.fromData(workInfo.progress),
                            )
                        }
                    }
                }
        }
    }

    fun runWorkerIfNotRun(communityRef: CommunityRef) {
        if (didRunWorker.value == true) {
            return
        }

        val request = OneTimeWorkRequestBuilder<OfflinePostFeedWork>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(OfflinePostFeedWork.makeInputData(communityRef))
            .build()

        val workManager = WorkManager.getInstance(context)

        workManager.enqueue(request)
        workRequestId.value = request.id
        didRunWorker.value = true
    }

    fun cancel() {
        val workManager = WorkManager.getInstance(context)
        workRequestId.value?.let {
            workManager.cancelWorkById(it)
        }
    }
}
