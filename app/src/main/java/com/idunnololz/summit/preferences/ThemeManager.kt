package com.idunnololz.summit.preferences

import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.lifecycle.MutableLiveData
import com.idunnololz.summit.R
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.BaseActivity
import com.idunnololz.summit.util.isLightTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    private val preferences: Preferences,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    private val coroutineScope = coroutineScopeFactory.create()

    val useMaterialYou = MutableStateFlow<Boolean>(preferences.isUseMaterialYou())
    val themeOverlayChanged = MutableSharedFlow<Unit>()

    fun applyThemeFromPreferences() {
        val themeValue = when (preferences.getBaseTheme()) {
            BaseTheme.UseSystem -> MODE_NIGHT_FOLLOW_SYSTEM
            BaseTheme.Light -> MODE_NIGHT_NO
            BaseTheme.Dark -> MODE_NIGHT_YES
        }

        if (AppCompatDelegate.getDefaultNightMode() != themeValue) {
            AppCompatDelegate.setDefaultNightMode(themeValue)
        }

        coroutineScope.launch {
            useMaterialYou.emit(preferences.isUseMaterialYou())
        }
    }

    fun onThemeOverlayChanged() {
        coroutineScope.launch {
            themeOverlayChanged.emit(Unit)
        }
    }

    fun applyThemeForActivity(activity: BaseActivity) {
        if (preferences.isBlackTheme()) {
            if (activity.isLightTheme()) {
                activity.theme.applyStyle(R.style.OverlayThemeRegular, true)
            }
        } else {
            activity.theme.applyStyle(R.style.OverlayThemeRegular, true)
        }

        when (preferences.globalFontSize) {
            GlobalFontSizeId.Small ->
                activity.theme.applyStyle(R.style.TextStyle_Small, true)
            GlobalFontSizeId.Normal ->
                activity.theme.applyStyle(R.style.TextStyle, true)
            GlobalFontSizeId.Large ->
                activity.theme.applyStyle(R.style.TextStyle_Large, true)
            GlobalFontSizeId.ExtraLarge ->
                activity.theme.applyStyle(R.style.TextStyle_ExtraLarge, true)
        }
    }

}