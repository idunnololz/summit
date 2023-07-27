package com.idunnololz.summit.offline

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.idunnololz.summit.util.Utils

class OfflineService : JobIntentService() {

    companion object {
        private const val TAG = "OfflineServer"

        private const val ARG_CONFIG = "ARG_CONFIG"

        fun newIntent(context: Context, config: OfflineTaskConfig): Intent =
            Intent(context, OfflineService::class.java).apply {
                putExtra(ARG_CONFIG, Utils.gson.toJson(config))
            }

        fun startWithConfig(context: Context, config: OfflineTaskConfig) {
            enqueueWork(context, OfflineService::class.java, 100, newIntent(context, config))
        }
    }

    override fun onHandleWork(intent: Intent) {
//        val notificationId = NotificationHelper.OFFLINE_NOTIFICATION_ID
//
//        val pendingIntent: PendingIntent =
//            Intent(this, MainActivity::class.java).let { notificationIntent ->
//                PendingIntent.getActivity(this, 0, notificationIntent, 0)
//            }
//
//        val builder = NotificationCompat.Builder(this, NotificationHelper.NORMAL_CHANNEL_ID)
//            .setContentTitle(getText(R.string.offline_notification_message))
//            .setContentText("")
//            .setSmallIcon(R.drawable.baseline_save_alt_black_24)
//            .setContentIntent(pendingIntent)
//            .setTicker(getText(R.string.offline_ticker_text))
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//
//        startForeground(notificationId, builder.build())
//
//        val percentFormat = NumberFormat.getPercentInstance()
//
//        val PROGRESS_MAX = 100
//        val PROGRESS_CURRENT = 0
//        NotificationManagerCompat.from(this).apply {
//            // Issue the initial notification with zero progress
//            builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, true)
//            notify(notificationId, builder.build())
//
//            // Do the job here that tracks the progress.
//            // Usually, this should be in a
//            // worker thread
//            // To show progress, update PROGRESS_CURRENT and update the notification with:
//            // builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
//            // notificationManager.notify(notificationId, builder.build());
//
//            val config = Utils.gson.fromJson(
//                intent.getStringExtra(ARG_CONFIG),
//                OfflineTaskConfig::class.java
//            )
//            val offlineManager = OfflineManager.instance
//            val listener = offlineManager.addOfflineDownloadProgressListener { message, progress ->
//                Log.d(TAG, "[${percentFormat.format(progress)}] $message")
//
//                if (progress == 1.0) {
//                    builder.setContentText("Download complete")
//                        .setProgress(0, 0, false)
//                    notify(notificationId, builder.build())
//                } else {
//                    builder
//                        .setProgress(PROGRESS_MAX, (progress * 100).toInt(), false)
//                    notify(notificationId, builder.build())
//                }
//            }
//            offlineManager.doOfflineBlocking(config)
//            offlineManager.removeOfflineDownloadProgressListener(listener)
//        }
    }
}
