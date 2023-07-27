package com.idunnololz.summit.connectivity

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.idunnololz.summit.actions.PendingActionsManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ConnectivityChangedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingActionsManager: PendingActionsManager,
) : Worker(appContext, workerParams) {

    companion object {
        private const val TAG = "ConnectivityChangedWork"
    }

    override fun doWork(): Result {
        Log.d(TAG, "Connectivity change detected!")

        pendingActionsManager.executePendingActionsIfNeeded()

        return Result.success()
    }
}
