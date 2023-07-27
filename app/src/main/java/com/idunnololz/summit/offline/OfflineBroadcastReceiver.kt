package com.idunnololz.summit.offline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.idunnololz.summit.util.Utils

class OfflineBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "OfflineBroadcastReceive"

        private const val ARG_CONFIG = "ARG_CONFIG"

        fun newIntent(context: Context, config: OfflineTaskConfig): Intent =
            Intent(context, OfflineBroadcastReceiver::class.java).apply {
                putExtra(ARG_CONFIG, Utils.gson.toJson(config))
            }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive")

        context ?: return

        val config =
            Utils.gson.fromJson(intent?.getStringExtra(ARG_CONFIG), OfflineTaskConfig::class.java)

        OfflineService.startWithConfig(context, config)
    }
}
