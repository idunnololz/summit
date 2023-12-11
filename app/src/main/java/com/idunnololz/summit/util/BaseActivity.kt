package com.idunnololz.summit.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.idunnololz.summit.MainApplication
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getColorFromAttribute
import io.github.inflationx.viewpump.ViewPumpContextWrapper

abstract class BaseActivity : AppCompatActivity() {

    companion object {
        private val TAG = BaseActivity::class.java.canonicalName
    }

    private var customNavigationBar = false
    private var navigationBarColor = 0

    private val logTag: String = javaClass.canonicalName ?: "UNKNOWN_CLASS"

    var isMaterialYou = false

    fun runOnUiThreadSafe(f: () -> Unit) {
        if (!isFinishing) {
            runOnUiThread {
                try {
                    f()
                } catch (e: IllegalStateException) { /* meh */
                }
            }
        }
    }

    override fun attachBaseContext(base: Context) {
        if (PreferenceUtil.usingCustomFont) {
            super.attachBaseContext(
                ViewPumpContextWrapper.wrap(
                    LocaleHelper.setLocale(base),
                ),
            )
        } else {
            super.attachBaseContext(LocaleHelper.setLocale(base))
        }
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            val uiMode = overrideConfiguration.uiMode
            overrideConfiguration.setTo(baseContext.resources.configuration)
            overrideConfiguration.uiMode = uiMode
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onPause() {
        super.onPause()
        MyLog.d(logTag, "onPause")
    }

    override fun onStop() {
        super.onStop()
        MyLog.d(logTag, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        MyLog.d(logTag, "onDestroy")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as MainApplication).themeManager.applyThemeForActivity(this)

        window.decorView.setBackgroundColor(getColorFromAttribute(android.R.attr.windowBackground))

        if (customNavigationBar) {
            window.navigationBarColor = navigationBarColor

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val red = Color.red(navigationBarColor)
                val green = Color.green(navigationBarColor)
                val blue = Color.blue(navigationBarColor)

                if (red * 0.299 + green * 0.587 + blue * 0.114 > 186) {
                    requestWindowFeature(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    var flags = window.decorView.systemUiVisibility
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    window.decorView.systemUiVisibility = flags
                }
            }
        }
        super.onCreate(savedInstanceState)
        MyLog.d(logTag, "onCreate")
    }

    override fun onStart() {
        super.onStart()
        MyLog.d(logTag, "onStart")
    }

    override fun onResume() {
        super.onResume()
        MyLog.d(logTag, "onResume")
    }
}
