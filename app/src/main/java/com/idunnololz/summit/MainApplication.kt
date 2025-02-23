package com.idunnololz.summit

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.transition.CrossfadeTransition
import coil.util.DebugLogger
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
import com.idunnololz.summit.util.LocaleHelper
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.coil.CustomVideoFrameDecoder
import com.idunnololz.summit.util.isFirebaseInitialized
import com.idunnololz.summit.video.ExoPlayerManager
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

    private var originalLocale: Locale? = null

    override fun attachBaseContext(base: Context) {
        PreferenceUtil.initialize(base)
        super.attachBaseContext(base)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        themeManager.updateTextConfig()

        onLocaleMightHaveChanged()
    }

    private fun onLocaleMightHaveChanged() {
        val newLocale = getString(R.string.locale)
        Log.d(TAG, "onConfigurationChanged() - locale: ${getString(R.string.locale)}")
    }

    override fun onCreate() {
        val context = applicationContext

        PreferenceUtil.initialize(context)

        val sharedPreferences = PreferenceUtil.preferences

        AppCompatDelegate.setDefaultNightMode(
            sharedPreferences.getInt(
                PreferenceUtil.KEY_THEME,
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

        var startTime = System.currentTimeMillis()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Perf. Crashlytics: " + (System.currentTimeMillis() - startTime))
            startTime = System.currentTimeMillis()
        }

        originalLocale = Locale.getDefault()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Perf. MobileAds: " + (System.currentTimeMillis() - startTime))
            startTime = System.currentTimeMillis()
        }

        // Needs to be initialized first
        ExoPlayerManager.initialize(context)
        DataFiles.initialize(context)
        DataCache.initialize(context)
        OfflineScheduleManager.initialize(context)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Perf. Everything else: " + (System.currentTimeMillis() - startTime))
            startTime = System.currentTimeMillis()
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Perf. setLocaleFromPrefs: " + (System.currentTimeMillis() - startTime))
        }

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

        Coil.setImageLoader(
            ImageLoader.Builder(context)
                .transitionFactory(
                    CrossfadeTransition.Factory(
                        durationMillis = IMAGE_LOAD_CROSS_FADE_DURATION_MS.toInt(),
                        preferExactIntrinsicSize = true,
                    ),
                )
                .okHttpClient(Client.get())
                .components {
                    if (SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                    add(CustomVideoFrameDecoder.Factory())
                    add(SvgDecoder.Factory())
                }
                .apply {
                    if (BuildConfig.DEBUG) {
                        logger(DebugLogger())
                    }
                }
                .build(),
        )

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
