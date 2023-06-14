package com.idunnololz.summit.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * [BroadcastReceiver] that handles events that might reset the alarms we register. We probably want
 * to check alarms are register them as necessary in this receiver.
 */
class AlarmResetReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = AlarmResetReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive. Intent.action: ${intent?.action}")

        // re-register alarms as needed

        // Actually we don't need to do anything here since the OS needs to create our app before it
        // can start this receiver. This will cause
        // MainApplication to be created which will in turn create OfflineScheduleManager and
        // re-register our alarms.
        //
        // :)
    }
}