package com.idunnololz.summit.preferences

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.app.ActivityCompat.recreate
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.presets.PreviewPresetActivity
import com.idunnololz.summit.util.BaseActivity
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.color.ColorManager
import com.idunnololz.summit.util.isLightTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.calligraphy3.TypefaceUtils
import io.github.inflationx.viewpump.ViewPump
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class ThemeManager @Inject constructor(
  @ApplicationContext private val context: Context,
  private val preferenceManager: PreferenceManager,
  private val accountManager: AccountManager,
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val colorManager: ColorManager,
  private val basePreferences: Preferences,
) {

  private val coroutineScope = coroutineScopeFactory.create()

  private var currentThemeOverlay: ThemeOverlayConfig? = null

  private var preferences: Preferences = basePreferences

  val useMaterialYou = MutableStateFlow<Boolean>(preferences.isUseMaterialYou)
  val themeOverlayChanged = MutableSharedFlow<Unit>()
  val useCustomFont = MutableStateFlow<Boolean>(preferences.globalFont != 0)
  var viewPump: ViewPump? = null

  var isLightTheme =
    when (preferences.baseTheme) {
      BaseTheme.UseSystem -> context.isLightTheme()
      BaseTheme.Light -> true
      BaseTheme.Dark -> false
    }
    private set

  init {
    coroutineScope.launch {
      accountManager.currentAccount.collect {
        withContext(Dispatchers.Main) {
          updatePreferences()
        }
      }
    }
  }

  fun updatePreferences() {
    preferences = preferenceManager.updateCurrentPreferences(accountManager.currentAccount.value)
    onPreferencesChanged()
  }

  fun onPreferencesChanged() {
    val currentConfig = ThemeOverlayConfig(
      baseTheme = preferences.baseTheme,
      isBlackTheme = preferences.isBlackTheme,
      useLessDarkBackgroundTheme = preferences.useLessDarkBackgroundTheme,
      isMaterialYou = preferences.isUseMaterialYou,
      colorScheme = preferences.colorScheme,
      globalFontSize = preferences.globalFontSize,
      globalFontColor = preferences.globalFontColor,
      globalFont = preferences.globalFont,
    )

    if (currentThemeOverlay == currentConfig) {
      return
    }
    currentThemeOverlay = currentConfig

    applyThemeFromPreferences(currentConfig)
    onThemeOverlayChanged(currentConfig)
  }

  fun updateTextConfig() {
    val currentConfig = currentThemeOverlay ?: return
    updateTextConfig(currentConfig)
  }

  fun updateTextConfig(currentConfig: ThemeOverlayConfig) {
    isLightTheme =
      when (currentConfig.baseTheme) {
        BaseTheme.UseSystem -> context.isLightTheme()
        BaseTheme.Light -> true
        BaseTheme.Dark -> false
      }
  }

  private fun applyThemeFromPreferences(currentConfig: ThemeOverlayConfig) {
    val fontAsset = currentConfig.globalFont.toFontAsset()
    if (fontAsset != null) {
      viewPump = ViewPump.builder()
        .addInterceptor(
          CalligraphyInterceptor(
            CalligraphyConfig.Builder()
              .setDefaultFontPath(fontAsset)
              .build(),
          ),
        )
        .build()

      val typeface = TypefaceUtils.load(context.assets, fontAsset)
      Utils.defaultTypeface = typeface
    } else {
      viewPump = null
      Utils.defaultTypeface = null
    }

    val themeValue = when (currentConfig.baseTheme) {
      BaseTheme.UseSystem -> MODE_NIGHT_FOLLOW_SYSTEM
      BaseTheme.Light -> MODE_NIGHT_NO
      BaseTheme.Dark -> MODE_NIGHT_YES
    }

    if (AppCompatDelegate.getDefaultNightMode() != themeValue) {
      AppCompatDelegate.setDefaultNightMode(themeValue)
    }

    coroutineScope.launch {
      useMaterialYou.emit(currentConfig.isMaterialYou)
      useCustomFont.emit(currentConfig.globalFont != 0)
      PreferenceKeys.usingCustomFont = useCustomFont.value
    }
  }

  private fun onThemeOverlayChanged(currentConfig: ThemeOverlayConfig) {
    coroutineScope.launch {
      themeOverlayChanged.emit(Unit)
      updateTextConfig(currentConfig)
    }
  }

  fun applyThemeForActivity(activity: BaseActivity) {
    val isPreviewActivity = activity is PreviewPresetActivity
    val preferences = if (isPreviewActivity) {
      basePreferences
    } else {
      preferences
    }

    if (activity.isLightTheme()) {
      // do nothing
    } else {
      if (preferences.useLessDarkBackgroundTheme) {
        activity.theme.applyStyle(R.style.LessDarkBackground, true)
      } else if (!preferences.isBlackTheme) {
        activity.theme.applyStyle(R.style.OverlayThemeRegular, true)
      }
    }

    when (preferences.globalFontSize) {
      GlobalFontSizeId.Small ->
        activity.theme.applyStyle(R.style.TextStyle_Small, true)
      GlobalFontSizeId.MediumSmall ->
        activity.theme.applyStyle(R.style.TextStyle_MediumSmall, true)
      GlobalFontSizeId.Normal ->
        activity.theme.applyStyle(R.style.TextStyle, true)
      GlobalFontSizeId.Large ->
        activity.theme.applyStyle(R.style.TextStyle_Large, true)
      GlobalFontSizeId.ExtraLarge ->
        activity.theme.applyStyle(R.style.TextStyle_ExtraLarge, true)
      GlobalFontSizeId.Xxl ->
        activity.theme.applyStyle(R.style.TextStyle_Xxl, true)
      GlobalFontSizeId.Xxxl ->
        activity.theme.applyStyle(R.style.TextStyle_Xxxl, true)
    }

    activity.isMaterialYou = if (isPreviewActivity) {
      preferences.isUseMaterialYou
    } else {
      useMaterialYou.value
    }
    if (activity.isMaterialYou) {
      DynamicColors.applyToActivityIfAvailable(activity)
    } else {
      // do nothing
    }

    when (preferences.colorScheme) {
      ColorSchemes.Blue -> {
        activity.theme.applyStyle(R.style.ThemeBlueOverlay, true)
      }
      ColorSchemes.Red -> {
        activity.theme.applyStyle(R.style.ThemeRedOverlay, true)
      }
      ColorSchemes.TalhaPurple -> {
        activity.theme.applyStyle(R.style.ThemeTalhaEPurpleOverlay, true)
      }
      ColorSchemes.TalhaGreen -> {
        activity.theme.applyStyle(R.style.ThemeTalhaEGreenOverlay, true)
      }
      ColorSchemes.TalhaPink -> {
        activity.theme.applyStyle(R.style.ThemeTalhaEPinkOverlay, true)
      }
      ColorSchemes.Peachie -> {
        activity.theme.applyStyle(R.style.ThemePeachieOverlay, true)
      }
      ColorSchemes.Fuchsia -> {
        activity.theme.applyStyle(R.style.ThemeFuchsiaOverlay, true)
      }
      ColorSchemes.Minty -> {
        activity.theme.applyStyle(R.style.ThemeMintyOverlay, true)
      }
      else -> { /* do nothing */ }
    }

    when (preferences.globalFontColor) {
      GlobalFontColorId.Calm ->
        activity.theme.applyStyle(R.style.TextColor, true)
      GlobalFontColorId.HighContrast ->
        activity.theme.applyStyle(R.style.TextColor_HighContrast, true)
    }

    colorManager.updateColors(activity)

    if (!isPreviewActivity) {
      activity.lifecycleScope.launch(Dispatchers.Default) {
        useMaterialYou.collect {
          withContext(Dispatchers.Main) {
            if (it != activity.isMaterialYou) {
              recreate(activity)
            }
          }
        }
      }
    }
  }

  data class ThemeOverlayConfig(
    val baseTheme: BaseTheme,
    val isBlackTheme: Boolean,
    val useLessDarkBackgroundTheme: Boolean,
    val isMaterialYou: Boolean,
    val colorScheme: ColorSchemeId,
    val globalFontSize: Int,
    val globalFontColor: Int,
    val globalFont: Int,
  )
}
