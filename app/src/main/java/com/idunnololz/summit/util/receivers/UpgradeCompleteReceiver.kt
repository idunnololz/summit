package com.idunnololz.summit.util.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UpgradeCompleteReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "UpgradeCompleteReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive. Intent.action: ${intent?.action}")
    }
}
