package com.idunnololz.summit.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.inbox.InboxEntriesDao
import com.idunnololz.summit.lemmy.inbox.InboxEntry
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.preferences.NotificationsSharedPreference
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.APP_STARTUP_DELAY_MS
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class NotificationsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val notificationsUpdaterFactory: NotificationsUpdater.Factory,
    private val accountManager: AccountManager,
    private val inboxEntriesDao: InboxEntriesDao,
    @NotificationsSharedPreference private val notificationsSharedPreferences: SharedPreferences,
) {
    companion object {
        private const val TAG = "NotificationsManager"

        private const val ChannelIdAccountPrefix = "channel.account."
        private const val ChannelGroupIdAccountPrefix = "channel_group.account."

        private const val InboxNotificationStartId = 1000
        private const val InboxNotificationLastId = 9999

        private const val AccountSummaryNotificationStartId = 20000
    }

    class NotificationWithId(
        val notificationId: Int,
        val notification: Notification,
    )

    private val coroutineScope = coroutineScopeFactory.create()
    private val workManager = WorkManager.getInstance(context)
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var channelsCreated = mutableSetOf<String>()

    private var nextNotificationId = if (preferences.lastAccountNotificationId == 0) {
        InboxNotificationStartId
    } else {
        preferences.lastAccountNotificationId
    }

    fun start() {
        coroutineScope.launch {
            delay(APP_STARTUP_DELAY_MS)
            enqueueWorkersIfNeeded()

            notificationsUpdaterFactory.create()
                .run()
        }
    }

    fun onPreferencesChanged() {
        enqueueWorkersIfNeeded()
    }

    fun reenqueue() {
        cancelWorkers()
        enqueueWorkersIfNeeded()
    }

    fun enqueueWorkersIfNeeded() {
        Log.d(TAG, "enqueueWorkersIfNeeded(): ${preferences.isNotificationsOn}")

        if (preferences.isNotificationsOn) {
            enqueueWorkers(preferences.notificationsCheckIntervalMs)
        } else {
            cancelWorkers()
        }
    }

    private fun enqueueWorkers(delayMs: Long) {
        if (!preferences.isNotificationsOn) {
            return
        }

        val repeatInterval = max(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, delayMs)
        val flexInterval = PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS

        val constraints = Constraints.Builder()
//            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work =
            PeriodicWorkRequestBuilder<NotificationsWorker>(
                repeatInterval,
                TimeUnit.MILLISECONDS, // repeatInterval (the period cycle)
                flexInterval,
                TimeUnit.MILLISECONDS, // flexInterval
            )
                .setInitialDelay(repeatInterval, TimeUnit.MILLISECONDS)
                .addTag(TAG)
                .setConstraints(constraints)
                .build()

        workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, work)

        val minuteMs = 1000 * 60
        Log.d(TAG, "Work scheduled to run in ${repeatInterval / minuteMs} minutes, every ${repeatInterval / minuteMs} minutes.")

        printDebugWorkersInfo()
    }

    private fun printDebugWorkersInfo() {
        if (!BuildConfig.DEBUG) return

        val minuteMs = 1000 * 60f

        val infos = workManager.getWorkInfosByTag(TAG).get()
        for (info in infos) {
            Log.d(TAG, "Worker ${info.id}")
            Log.d(TAG, "Work in ${(info.nextScheduleTimeMillis - System.currentTimeMillis()) / minuteMs} minutes")
        }
    }

    private fun cancelWorkers() {
        Log.d(TAG, "all workers cancelled")
        workManager.cancelAllWorkByTag(TAG)
    }

    private fun createNotificationChannelIfNeededForAccount(account: Account) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channelId = getChannelIdForAccount(account)
        if (channelsCreated.contains(channelId)) {
            return
        }

        channelsCreated.add(channelId)

        val groupKey = getChannelGroupIdForAccount(account)

        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(
                groupKey,
                account.fullName,
            ),
        )

        val name = context.getString(R.string.account_notifications)
        val descriptionText = context.getString(R.string.account_notifications_desc)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(
            channelId,
            name,
            importance,
        ).apply {
            description = descriptionText
            group = groupKey
        }
        // Register the channel with the system
        notificationManager.createNotificationChannel(channel)
    }

    fun showNotificationsForItems(account: Account, newItems: List<InboxItem>) {
        if (newItems.isEmpty()) {
            return
        }

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                createNotificationChannelIfNeededForAccount(account)
            }

            val channelId = getChannelIdForAccount(account)
            val channelGroupId = getChannelGroupIdForAccount(account)

            val newInboxNotificationItems = mutableListOf<NotificationWithId>()

            val notificationSummaryInfo = NotificationCompat.InboxStyle()

            val summaryNotificationBuilder = NotificationCompat.Builder(context, channelId)
                .setContentTitle(account.fullName)
                // Set content text to support devices running API level < 24.
                .setContentText(
                    context.resources.getQuantityString(
                        R.plurals.new_items_in_inbox_format,
                        newItems.size,
                        newItems.size.toString(),
                    ),
                )
                .setSmallIcon(R.drawable.ic_logo_mono_24)

            newItems.forEach { inboxItem ->

                val notificationId = nextNotificationId

                if (nextNotificationId == InboxNotificationLastId) {
                    nextNotificationId = InboxNotificationStartId
                }

                nextNotificationId++

                val title: String = inboxItem.title
                val body: String = inboxItem.content
                val authorAvatar: String? = inboxItem.authorAvatar

                notificationSummaryInfo.addLine(title)

                val intent = MainActivity.createInboxItemIntent(context, account, notificationId)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    nextNotificationId,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT,
                )

                val newMessageNotification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_logo_mono_24)
                    .setContentTitle(title)
                    .setContentText(body)
//                .setLargeIcon(Icon.cre)
                    .setGroup(channelGroupId)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_EMAIL)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                newInboxNotificationItems.add(
                    NotificationWithId(
                        notificationId = notificationId,
                        notification = newMessageNotification,
                    ),
                )

                withContext(Dispatchers.IO) {
                    inboxEntriesDao.insertEntry(
                        InboxEntry(
                            id = 0,
                            ts = System.currentTimeMillis(),
                            itemId = inboxItem.id,
                            notificationId = notificationId,
                            accountFullName = account.fullName,
                            inboxItem = inboxItem,
                        ),
                    )
                }
            }

            preferences.lastAccountNotificationId = nextNotificationId

            withContext(Dispatchers.IO) {
                inboxEntriesDao.pruneDb()
            }

            notificationSummaryInfo
                .setBigContentTitle(
                    context.resources.getQuantityString(
                        R.plurals.new_items_in_inbox_format,
                        newItems.size,
                        newItems.size.toString(),
                    ),
                )
                .setSummaryText(account.fullName)

            val localAccountId = accountManager.getLocalAccountId(account)
            val summaryNotificationId = AccountSummaryNotificationStartId + localAccountId

            val intent = MainActivity.createInboxPageIntent(context, account)
            val pendingIntent = PendingIntent.getActivity(
                context,
                summaryNotificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT,
            )

            val summaryNotification = summaryNotificationBuilder
                // Build summary info into InboxStyle template.
                .setStyle(notificationSummaryInfo)
                // Specify which group this notification belongs to.
                .setGroup(channelGroupId)
                // Set this notification as the summary for the group.
                .setGroupSummary(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            Log.d(TAG, "group key: $channelGroupId. localAccountId: $localAccountId")

            NotificationManagerCompat.from(context).apply {
                try {
                    newInboxNotificationItems.forEach {
                        Log.d(TAG, "Creating new notification. NoteId: ${it.notificationId}")
                        notify(it.notificationId, it.notification)
                    }
                    notify(summaryNotificationId, summaryNotification)
                } catch (e: SecurityException) {
                    preferences.isNotificationsOn = false
                    onPreferencesChanged()
                }
            }
        }
    }

    suspend fun findInboxItem(notificationId: Int): InboxEntry? {
        return inboxEntriesDao.findInboxEntriesByNotificationId(notificationId).firstOrNull {
            it.inboxItem != null
        }
    }

    private fun getChannelIdForAccount(account: Account) =
        ChannelIdAccountPrefix + account.fullName

    private fun getChannelGroupIdForAccount(account: Account) =
        ChannelGroupIdAccountPrefix + account.fullName

    fun isNotificationsEnabledForAccount(account: Account): Boolean =
        notificationsSharedPreferences.getBoolean("${account.fullName}_isOn", false)

    fun setNotificationsEnabledForAccount(account: Account, isEnabled: Boolean) {
        notificationsSharedPreferences.edit()
            .putBoolean("${account.fullName}_isOn", isEnabled)
            .apply()
    }

    fun getLastNotificationItemTsForAccount(account: Account) =
        notificationsSharedPreferences.getLong("${account.fullName}_lastItemTs", 0L)

    fun setLastNotificationItemTsForAccount(account: Account, ts: Long) {
        notificationsSharedPreferences.edit()
            .putLong("${account.fullName}_lastItemTs", ts)
            .apply()
    }

    fun removeAllInboxNotificationsForAccount(account: Account) {
        coroutineScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val notifications = notificationManager.activeNotifications
                val channelGroupId = getChannelGroupIdForAccount(account)
                val accountNotifications = notifications.filter {
                    it.groupKey == channelGroupId
                }
                notifications.forEach {
                    notificationManager.cancel(it.id)
                }
            }
        }
    }

    fun removeNotificationForInboxItem(inboxItem: InboxItem, account: Account) {
        coroutineScope.launch {
            val items = inboxEntriesDao.findInboxEntriesByItemId(inboxItem.id)
            val entry = items.firstOrNull {
                it.itemId == inboxItem.id && it.accountFullName == account.fullName
            }

            if (entry != null) {
                notificationManager.cancel(entry.notificationId)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val notifications = notificationManager.activeNotifications
                    val channelGroupId = getChannelGroupIdForAccount(account)
                    val accountNotifications = notifications.filter {
                        it.groupKey == channelGroupId
                    }
                    val groupNotification = notifications.firstOrNull { it.isGroup }
                    val otherNotifications = notifications.filter {
                        !it.isGroup && it.id != entry.notificationId
                    }

                    if (otherNotifications.isEmpty() && groupNotification != null) {
                        notificationManager.cancel(groupNotification.id)
                    }
                }
            }
        }
    }
}
