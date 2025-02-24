package com.idunnololz.summit.util

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import com.idunnololz.summit.R
import java.util.*

object LocaleHelper {

    fun getLocaleFromPreferences(context: Context): Locale? {
        val settings = PreferenceUtils.preferences
        val localeStr = settings.getString(context.getString(R.string.pref_key_app_language), "")

        if (localeStr.isNullOrEmpty() || localeStr == "0") return null

        var region: String? = null
        val lang: String
        val split = localeStr.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (split.size > 1) {
            lang = split[0]
            region = split[1]
        } else {
            lang = localeStr
        }

        return if (region == null) {
            Locale(lang)
        } else {
            Locale(lang, region)
        }
    }

    fun setLocale(base: Context): Context {
        val locale = getLocaleFromPreferences(base)

        if (locale == null) return base

        Locale.setDefault(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResourcesLocale(base, locale)
        } else {
            updateResourcesLocaleLegacy(base, locale)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun updateResourcesLocale(context: Context, locale: Locale): Context {
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    private fun updateResourcesLocaleLegacy(context: Context, locale: Locale): Context {
        val resources = context.resources

        val configuration = resources.configuration
        configuration.locale = locale
        configuration.setLayoutDirection(locale)

        resources.updateConfiguration(configuration, resources.displayMetrics)

        return context
    }
}
