package com.idunnololz.summit.notifications

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.idunnololz.summit.MainApplication

class NotificationsWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {
        private const val TAG = "NotificationsWorker"
    }

    override fun doWork(): Result {
        Log.d(TAG, "doWork()")

        (applicationContext as? MainApplication)?.runNotificationsUpdate()

        return Result.success()
    }
}