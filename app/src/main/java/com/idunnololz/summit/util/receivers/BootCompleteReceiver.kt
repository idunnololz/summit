package com.idunnololz.summit.util.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * [BroadcastReceiver] that handles events that might reset the alarms we register. We probably want
 * to check alarms are register them as necessary in this receiver.
 */
class BootCompleteReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompleteReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive. Intent.action: ${intent?.action}")
    }
}
