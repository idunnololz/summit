package com.idunnololz.summit

import android.app.Application
import android.content.Context
import android.content.res.Configuration
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
import coil3.util.DebugLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.notifications.NotificationsUpdater
import com.idunnololz.summit.offline.OfflineScheduleManager
import com.idunnololz.summit.preferences.GlobalSettings
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.util.AnimationUtils.IMAGE_LOAD_CROSS_FADE_DURATION_MS
import com.idunnololz.summit.util.Client
import com.idunnololz.summit.util.DataCache
import com.idunnololz.summit.util.DataFiles
import com.idunnololz.summit.util.PreferenceUtils
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.coil3.video.VideoFrameDecoder
import com.idunnololz.summit.util.isFirebaseInitialized
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
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

//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return
//        }
//        LeakCanary.install(this)


        // Needs to be initialized first
        DataFiles.initialize(context)
        DataCache.initialize(context)
        OfflineScheduleManager.initialize(context)

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

        OfflineScheduleManager.instance.setupAlarms()

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
                                Client.get()
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
                        logger(DebugLogger())
                    }
                }
                .build()
        }

        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(this, AppEntryPoint::class.java)
        val preferences = hiltEntryPoint.preferences()

        hiltEntryPoint.themeManager().onPreferencesChanged()
        Utils.openExternalLinksInBrowser = preferences.openLinksInExternalApp
        LemmyTextHelper.autoLinkPhoneNumbers = preferences.autoLinkPhoneNumbers
        LemmyTextHelper.autoLinkIpAddresses = preferences.autoLinkIpAddresses
        notificationsUpdaterFactory = hiltEntryPoint.notificationsUpdaterFactory()

        hiltEntryPoint.notificationsManager().start()

        GlobalSettings.refresh(preferences)

        hiltEntryPoint.accountInfoManager().init()
        hiltEntryPoint.conversationsManager().init()

        if (preferences.useFirebase) {
            FirebaseApp.initializeApp(this)
            Firebase.crashlytics.isCrashlyticsCollectionEnabled = true

            isFirebaseInitialized = true
        }
    }

    fun runNotificationsUpdate() {
        notificationsUpdaterFactory?.create()?.run()
    }

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
