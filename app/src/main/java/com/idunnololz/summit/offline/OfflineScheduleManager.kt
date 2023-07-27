package com.idunnololz.summit.offline

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.idunnololz.summit.util.PreferenceUtil
import java.text.SimpleDateFormat
import java.util.*

/**
 * A class that manages scheduled events related to offline features. Eg. download offline data
 * every day at 1am.
 */
class OfflineScheduleManager(
    private val context: Context,
) {

    companion object {

        private const val TAG = "OfflineScheduleManager"

        @SuppressLint("StaticFieldLeak") // application context
        lateinit var instance: OfflineScheduleManager
            private set

        fun initialize(context: Context) {
            instance = OfflineScheduleManager(context.applicationContext)
        }

        private const val OFFLINE_DOWNLOAD_REQUEST_CODE = 1001
    }

    private val preferences = PreferenceUtil.preferences

    fun setupAlarms() {
        val isOfflineSchedulerEnabled =
            preferences.getBoolean(PreferenceUtil.KEY_ENABLE_OFFLINE_SCHEDULE, false)
        if (isOfflineSchedulerEnabled) {
            val olderIntent = PendingIntent.getBroadcast(
                context,
                OFFLINE_DOWNLOAD_REQUEST_CODE,
                getOfflineIntent(),
                PendingIntent.FLAG_NO_CREATE,
            )
            if (olderIntent == null) {
                Log.d(TAG, "There were no pending intents registered. Registering a new one...")

                onAlarmChanged()
            } else {
                Log.d(TAG, "Alarm already registered.")
            }
        }
    }

    fun onAlarmChanged() {
        Log.d(TAG, "Alarm changed...")

        val isOfflineSchedulerEnabled =
            preferences.getBoolean(PreferenceUtil.KEY_ENABLE_OFFLINE_SCHEDULE, false)
        val recurringEvent = preferences.getString(PreferenceUtil.KEY_OFFLINE_SCHEDULE, null)?.let {
            RecurringEvent.fromString(it)
        } ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, recurringEvent.hourOfDay)
            set(Calendar.MINUTE, recurringEvent.minuteOfHour)
        }

        val alarmIntent = PendingIntent.getBroadcast(
            context,
            OFFLINE_DOWNLOAD_REQUEST_CODE,
            getOfflineIntent(),
            0,
        )

        // Cancel any existing alarms
        alarmManager?.cancel(alarmIntent)

        if (!isOfflineSchedulerEnabled) {
            Log.d(TAG, "Alarm is disabled...")
            return
        }

        // With setInexactRepeating(), you have to use one of the AlarmManager interval
        // constants--in this case, AlarmManager.INTERVAL_DAY.
        alarmManager?.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            alarmIntent,
        )
        Log.d(
            TAG,
            "Alarm set for ${
            SimpleDateFormat(
                "dd/MM/yy hh:mm aa",
                Locale.US,
            ).format(Date(calendar.timeInMillis))
            }",
        )
    }

    private fun getOfflineIntent(): Intent = OfflineBroadcastReceiver.newIntent(
        context,
        OfflineTaskConfig(
            minPosts = 100,
            roundPostsToNearestPage = true,
        ),
    )
}
