package com.idunnololz.summit.settings.theme

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.DynamicColors
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.preferences.BaseTheme
import com.idunnololz.summit.preferences.ColorSchemes
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.PreferencesViewModel
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.SubgroupItem
import com.idunnololz.summit.settings.ThemeSettings
import com.idunnololz.summit.settings.util.asColorItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import com.idunnololz.summit.settings.util.asRadioGroup
import com.idunnololz.summit.settings.util.asSingleChoiceSelectorItem
import com.idunnololz.summit.util.ext.getColorCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsThemeFragment : BaseSettingsFragment() {

  private val args: SettingsThemeFragmentArgs by navArgs()

  private val preferencesViewModel: PreferencesViewModel by viewModels()

  @Inject
  lateinit var themeManager: ThemeManager

  @Inject
  override lateinit var settings: ThemeSettings

  @Inject
  lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    preferences = preferencesViewModel.getPreferences(args.account)
  }

  override fun generateData(): List<SettingModelItem> {
    val account = args.account
    val context = requireContext()

    return listOf(
      settings.baseTheme.asRadioGroup(
        getCurrentValue = {
          when (preferences.baseTheme) {
            BaseTheme.UseSystem -> R.id.setting_option_use_system
            BaseTheme.Light -> R.id.setting_option_light_theme
            BaseTheme.Dark -> R.id.setting_option_dark_theme
          }
        },
        onValueChanged = {
          when (it) {
            R.id.setting_option_use_system ->
              preferences.baseTheme = BaseTheme.UseSystem
            R.id.setting_option_light_theme ->
              preferences.baseTheme = BaseTheme.Light
            R.id.setting_option_dark_theme ->
              preferences.baseTheme = BaseTheme.Dark
          }

          binding.root.post {
            themeManager.onPreferencesChanged()
          }
        },
      ),
      SettingModelItem.SubgroupItem(
        getString(R.string.theme_config),
        listOf(
          settings.materialYou.asOnOffSwitch(
            { preferences.isUseMaterialYou },
            {
              if (DynamicColors.isDynamicColorAvailable()) {
                preferences.isUseMaterialYou = it
                preferences.colorScheme = ColorSchemes.Default
                themeManager.onPreferencesChanged()
              } else if (it) {
                OldAlertDialogFragment.Builder()
                  .setMessage(R.string.error_dynamic_color_not_supported)
                  .createAndShow(childFragmentManager, "Asdfff")
                preferences.isUseMaterialYou = false
              } else {
                preferences.isUseMaterialYou = false
              }
            },
          ),
          settings.colorScheme.asCustomItem(
            { preferences.colorScheme },
            {
              ColorSchemePickerDialogFragment.newInstance(account)
                .show(childFragmentManager, "asdaa")
            },
          ),
        ),
      ),
      SettingModelItem.SubgroupItem(
        getString(R.string.dark_theme_settings),
        listOf(
          settings.blackTheme.asOnOffSwitch(
            { preferences.isBlackTheme },
            {
              preferences.isBlackTheme = it
              themeManager.onPreferencesChanged()
            },
          ),
          settings.lessDarkBackgroundTheme.asOnOffSwitch(
            { preferences.useLessDarkBackgroundTheme },
            {
              preferences.useLessDarkBackgroundTheme = it
              themeManager.onPreferencesChanged()
            },
          ),
        ),
      ),
      SettingModelItem.SubgroupItem(
        getString(R.string.font_style),
        listOf(
          settings.font.asCustomItem(
            { preferences.globalFont },
            {
              FontPickerDialogFragment.newInstance(account)
                .show(childFragmentManager, "FontPickerDialogFragment")
            },
          ),
          settings.fontSize.asSingleChoiceSelectorItem(
            {
              preferences.globalFontSize
            },
            {
              preferences.globalFontSize = it
              themeManager.onPreferencesChanged()
            },
          ),
          settings.fontColor.asSingleChoiceSelectorItem(
            {
              preferences.globalFontColor
            },
            {
              preferences.globalFontColor = it
              themeManager.onPreferencesChanged()
            },
          ),
        ),
      ),
      SettingModelItem.SubgroupItem(
        getString(R.string.vote_colors),
        listOf(
          settings.upvoteColor.asColorItem(
            { preferences.upvoteColor },
            {
              preferences.upvoteColor = it
              postAndCommentViewBuilder.onPreferencesChanged()
            },
            { context.getColorCompat(R.color.upvoteColor) },
          ),
          settings.downvoteColor.asColorItem(
            { preferences.downvoteColor },
            {
              preferences.downvoteColor = it
              postAndCommentViewBuilder.onPreferencesChanged()
            },
            { context.getColorCompat(R.color.downvoteColor) },
          ),
          BasicSettingItem(
            null,
            context.getString(R.string.swap_colors),
            null,
            id = R.id.swap_colors,
          ).asCustomItem(
            {
              val upvoteColor = preferences.downvoteColor
              val downvoteColor = preferences.upvoteColor

              preferences.upvoteColor = upvoteColor
              preferences.downvoteColor = downvoteColor

              postAndCommentViewBuilder.onPreferencesChanged()
            },
          ),
        ),
      ),
      SettingModelItem.SubgroupItem(
        getString(R.string.misc),
        listOf(
          settings.transparentNotificationBar.asOnOffSwitch(
            { preferences.transparentNotificationBar },
            {
              preferences.transparentNotificationBar = it
              getMainActivity()?.onPreferencesChanged()
            },
          ),
        ),
      ),
    )
  }
}
