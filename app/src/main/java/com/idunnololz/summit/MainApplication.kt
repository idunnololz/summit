package com.idunnololz.summit

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.transitionFactory
import coil3.svg.SvgDecoder
import coil3.transition.CrossfadeTransition
import com.idunnololz.summit.notifications.NotificationsUpdater
import com.idunnololz.summit.preferences.GlobalSettings
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.util.AnimationUtils.IMAGE_LOAD_CROSS_FADE_DURATION_MS
import com.idunnololz.summit.util.PreferenceUtils
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.coil.BetterDebugLogger
import com.idunnololz.summit.util.coil3.video.VideoFrameDecoder
import com.idunnololz.summit.util.crashLogger.isCrashLoggerInitialized
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import io.sentry.Hint
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application(), androidx.work.Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var preferences: Preferences

    private var notificationsUpdaterFactory: NotificationsUpdater.Factory? = null

    companion object {
        private val TAG = MainApplication::class.java.simpleName
    }

    override fun attachBaseContext(base: Context) {
        PreferenceUtils.initialize(base)
        super.attachBaseContext(base)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        Log.d(TAG, "LOCALE >> " + newConfig.locale)
        themeManager.updateTextConfig()
    }

    override fun onCreate() {
        val context = applicationContext

        PreferenceUtils.initialize(context)

        val sharedPreferences = PreferenceUtils.preferences

        AppCompatDelegate.setDefaultNightMode(
            sharedPreferences.getInt(
                PreferenceUtils.KEY_THEME,
                AppCompatDelegate.MODE_NIGHT_YES,
            ),
        )

        super.onCreate()

//        if (BuildConfig.DEBUG) {
//            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
//                    .detectDiskReads()
//                    .detectDiskWrites()
//                    .detectNetwork()   // or .detectAll() for all detectable problems
//                    .penaltyLog()
//                    .penaltyDeath()
//                    .build())
//            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
//                    .detectLeakedSqlLiteObjects()
//                    .detectLeakedClosableObjects()
//                    .detectActivityLeaks()
//                    .penaltyLog()
//                    .penaltyDeath()
//                    .build())
//        }

        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(this, AppEntryPoint::class.java)
        val preferences = hiltEntryPoint.preferences()

        hiltEntryPoint.themeManager().onPreferencesChanged()
        Utils.openExternalLinksInBrowser = preferences.openLinksInExternalApp
        Utils.defaultWebApp = preferences.defaultWebApp
        notificationsUpdaterFactory = hiltEntryPoint.notificationsUpdaterFactory()

        SingletonImageLoader.setSafe {
            ImageLoader.Builder(context)
                .transitionFactory(
                    CrossfadeTransition.Factory(
                        durationMillis = IMAGE_LOAD_CROSS_FADE_DURATION_MS.toInt(),
                        preferExactIntrinsicSize = true,
                    ),
                )
                .components {
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = {
                                hiltEntryPoint.browserLikeOkHttpClient()
                            },
                        ),
                    )
                }
                .components {
                    if (SDK_INT >= 28) {
                        add(AnimatedImageDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                    add(VideoFrameDecoder.Factory())
                    add(SvgDecoder.Factory())
                }
                .apply {
                    if (BuildConfig.DEBUG) {
                        logger(BetterDebugLogger())
                    }
                }
                .build()
        }

        correctDefaultWebApp()

        hiltEntryPoint.notificationsManager().start()

        GlobalSettings.refresh(preferences)

        hiltEntryPoint.accountInfoManager().init()
        hiltEntryPoint.conversationsManager().init()

        if (preferences.useFirebase) {
            SentryAndroid.init(this) { options ->
                options.dsn = "https://decaf7c0aa19b1c009de539fb95ad4a4@o4509018554630144.ingest.us.sentry.io/4509018556465152"
                // Add a callback that will be used before the event is sent to Sentry.
                // With this callback, you can modify the event or, when returning null, also discard the event.
                options.beforeSend =
                    SentryOptions.BeforeSendCallback { event: SentryEvent, hint: Hint ->
                        if (SentryLevel.DEBUG == event.level) {
                            null
                        } else {
                            event
                        }
                    }
                options.isAttachScreenshot = false
                options.isAttachViewHierarchy = false
                options.isEnableUserInteractionTracing = false
                options.isEnableUserInteractionBreadcrumbs = false
            }

            isCrashLoggerInitialized = true
        }
    }

    private fun correctDefaultWebApp() {
        val defaultWebApp = Utils.defaultWebApp

        if (defaultWebApp == null || defaultWebApp.componentName != null) {
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://google.com")
        }
        val pm = packageManager
        val options: List<ResolveInfo> =
            if (SDK_INT >= Build.VERSION_CODES.M) {
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            } else {
                pm.queryIntentActivities(intent, 0)
            }.mapNotNull { it }

        val option = options.filter {
            it.activityInfo.applicationInfo.packageName == defaultWebApp.packageName
        }

        if (option.isEmpty()) {
            return
        }

        val fixedDefaultApp = defaultWebApp.copy(
            componentName = option.first().activityInfo.name,
        )
        preferences.defaultWebApp = fixedDefaultApp
        Utils.defaultWebApp = fixedDefaultApp
    }

    fun runNotificationsUpdate() {
        notificationsUpdaterFactory?.create()?.run()
    }

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
