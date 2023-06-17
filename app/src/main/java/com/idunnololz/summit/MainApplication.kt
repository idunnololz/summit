package com.idunnololz.summit

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.gms.security.ProviderInstaller
import com.idunnololz.summit.auth.RedditAuthManager
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.offline.OfflineScheduleManager
import com.idunnololz.summit.reddit.*
import com.idunnololz.summit.reddit_actions.ActionInfo
import com.idunnololz.summit.reddit_actions.RedditAction
import com.idunnololz.summit.tabs.TabsManager
import com.idunnololz.summit.util.*
import com.idunnololz.summit.video.ExoPlayerManager
import dagger.hilt.android.HiltAndroidApp
import io.reactivex.plugins.RxJavaPlugins
import java.util.*

@HiltAndroidApp
class MainApplication : Application() {

    companion object {
        private val TAG = MainApplication::class.java.simpleName

        fun setLocaleFromPrefs(context: Context) {
            val locale = LocaleHelper.getLocaleFromPreferences(context)
            Log.d(TAG, "Lang: " + locale?.toString())
            Log.d(TAG, "Current locale: " + Locale.getDefault().toString())
            setLocale(context, locale)
        }

        fun setLocale(context: Context, locale: Locale?) {
            if (locale == null) return

            val config = context.resources.configuration

            val isAlreadyThatLocale: Boolean
            val curLocale: Locale = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                config.locales.get(0)
            } else {
                config.locale
            }
            isAlreadyThatLocale = curLocale.language.equals(locale.language, ignoreCase = true)
                    && curLocale.country.equals(locale.country, ignoreCase = true)

            if (isAlreadyThatLocale) {
                Log.d(TAG, "Looks like the locale did not change. Skipping locale set.")
                return
            }

            Locale.setDefault(locale)
            val conf = Configuration(config)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                conf.setLocale(locale)
            } else {
                conf.locale = locale
            }
            context.resources.updateConfiguration(
                conf,
                context.resources.displayMetrics
            )
        }
    }

    private var originalLocale: Locale? = null

    override fun attachBaseContext(base: Context) {
        PreferenceUtil.initialize(base)
        setLocaleFromPrefs(base)
        super.attachBaseContext(LocaleHelper.setLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setLocaleFromPrefs(this)
        LocaleHelper.setLocale(this)

        onLocaleMightHaveChanged()
    }

    private fun onLocaleMightHaveChanged() {
        val newLocale = getString(R.string.locale)
        Log.d(TAG, "onConfigurationChanged() - locale: ${getString(R.string.locale)}")
    }

    override fun onCreate() {
        val context = applicationContext

        PreferenceUtil.initialize(context)

        val preferences = PreferenceUtil.preferences

        AppCompatDelegate.setDefaultNightMode(
            preferences.getInt(
                PreferenceUtil.KEY_THEME, AppCompatDelegate.MODE_NIGHT_YES
            )
        )

        RxJavaPlugins.setErrorHandler {
            Log.e(TAG, "RxGlobalExceptionHandler", it)
        }

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

        checkTls()

        NotificationHelper.registerNotificationChannels(context)

        // Needs to be initialized first
        ExoPlayerManager.initialize(context)
        TabsManager.initialize(context)
        DataFiles.initialize(context)
        DataCache.initialize(context)
        OfflineManager.initialize(context)
        OfflineScheduleManager.initialize(context)
        RedditAuthManager.initialize(context)
        PendingActionsManager.initialize(context)
        RecentSubredditsManager.initialize(context)
        HistoryManager.initialize(context)
        LikesManager.initialize(context)
        PendingCommentsManager.initialize(context)
        PendingEditsManager.initialize(context)

        Fresco.initialize(this)

        val likesManager = LikesManager.instance
        PendingActionsManager.instance.addActionCompleteListener(object :
            PendingActionsManager.OnActionChangedListener {
            override fun onActionAdded(action: RedditAction) {
                when (action.info) {
                    is ActionInfo.VoteActionInfo -> {
                        likesManager.setPendingLike(action.info.id, action.info.dir)
                    }
                    is ActionInfo.CommentActionInfo -> {}
                    is ActionInfo.DeleteCommentActionInfo -> {}
                    is ActionInfo.EditActionInfo -> {}
                    is ActionInfo.UnknownActionInfo -> {}
                }
            }

            override fun onActionFailed(action: RedditAction) {
                when (action.info) {
                    is ActionInfo.VoteActionInfo -> {
                        likesManager.clearPendingLike(action.info.id)
                    }
                    is ActionInfo.CommentActionInfo -> {}
                    is ActionInfo.DeleteCommentActionInfo -> {}
                    is ActionInfo.EditActionInfo -> {}
                    is ActionInfo.UnknownActionInfo -> {}
                }
            }

            override fun onActionComplete(action: RedditAction) {
                when (action.info) {
                    is ActionInfo.VoteActionInfo -> {
                        likesManager.setLike(action.info.id, action.info.dir)
                    }
                    is ActionInfo.CommentActionInfo -> {}
                    is ActionInfo.DeleteCommentActionInfo -> {}
                    is ActionInfo.EditActionInfo -> {}
                    is ActionInfo.UnknownActionInfo -> {}
                }
            }
        })

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Perf. Everything else: " + (System.currentTimeMillis() - startTime))
            startTime = System.currentTimeMillis()
        }

        setLocaleFromPrefs(baseContext)

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
    }

    private fun checkTls() {
        if (Build.VERSION.SDK_INT < 21) {
            try {
                ProviderInstaller.installIfNeeded(this)
            } catch (e: Exception) {
                Log.e(TAG, "", e)
            }
        }
    }
}
