package com.idunnololz.summit.connectivity

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.idunnololz.summit.reddit.PendingActionsManager

class ConnectivityChangedWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    companion object {
        private const val TAG = "ConnectivityChangedWork"
    }

    override fun doWork(): Result {
        Log.d(TAG, "Connectivity change detected!")

        PendingActionsManager.instance.executePendingActionsIfNeeded()

        return Result.success()
    }
}