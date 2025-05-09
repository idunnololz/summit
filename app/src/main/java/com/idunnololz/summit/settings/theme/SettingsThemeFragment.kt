package com.idunnololz.summit.settings.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.color.DynamicColors
import com.idunnololz.summit.R
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingsGenericBinding
import com.idunnololz.summit.databinding.FragmentSettingsThemeBinding
import com.idunnololz.summit.databinding.RadioGroupOptionSettingItemBinding
import com.idunnololz.summit.databinding.RadioGroupTitleSettingItemBinding
import com.idunnololz.summit.databinding.SettingColorItemBinding
import com.idunnololz.summit.databinding.SettingItemOnOffBinding
import com.idunnololz.summit.databinding.SettingTextValueBinding
import com.idunnololz.summit.databinding.SubgroupSettingItemBinding
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.preferences.BaseTheme
import com.idunnololz.summit.preferences.ColorSchemes
import com.idunnololz.summit.preferences.GlobalFontColorId
import com.idunnololz.summit.preferences.GlobalFontSizeId
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.ColorSettingItem
import com.idunnololz.summit.settings.OnOffSettingItem
import com.idunnololz.summit.settings.PreferencesViewModel
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.settings.RadioGroupSettingItem.RadioGroupOption
import com.idunnololz.summit.settings.SettingItem
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsAdapter
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.SubgroupItem
import com.idunnololz.summit.settings.TextOnlySettingItem
import com.idunnololz.summit.settings.TextValueSettingItem
import com.idunnololz.summit.settings.ThemeSettings
import com.idunnololz.summit.settings.asColorItem
import com.idunnololz.summit.settings.asCustomItem
import com.idunnololz.summit.settings.asOnOffSwitch
import com.idunnololz.summit.settings.asRadioGroup
import com.idunnololz.summit.settings.asSingleChoiceSelectorItem
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.settings.util.bindToMultiView
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.SummitActivity
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.LinkedList
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
    val parentActivity = requireSummitActivity()
    val account = args.account
    val context = requireContext()

    return listOf(
      SettingModelItem.SubgroupItem(
        SubgroupItem(getString(R.string.base_theme), listOf(), listOf()),
        listOf(
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
          )
        )
      ),
      SettingModelItem.SubgroupItem(
        SubgroupItem(getString(R.string.theme_config), listOf(), listOf()),
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
          )
        )
      ),
      SettingModelItem.SubgroupItem(
        SubgroupItem(getString(R.string.dark_theme_settings), listOf(), listOf()),
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
          )
        )
      ),
      SettingModelItem.SubgroupItem(
        SubgroupItem(getString(R.string.font_style), listOf(), listOf()),
        listOf(
          settings.font.asCustomItem(
            { preferences.globalFont },
            {
              FontPickerDialogFragment.newInstance(account)
                .show(childFragmentManager, "FontPickerDialogFragment")
            },
          ),
          settings.fontSize.asSingleChoiceSelectorItem(
            activity = parentActivity,
            {
              preferences.globalFontSize
            },
            {
              preferences.globalFontSize = it
              themeManager.onPreferencesChanged()
            },
          ),
          settings.fontColor.asSingleChoiceSelectorItem(
            activity = parentActivity,
            {
              preferences.globalFontColor
            },
            {
              preferences.globalFontColor = it
              themeManager.onPreferencesChanged()
            },
          )
        )
      ),
      SettingModelItem.SubgroupItem(
        SubgroupItem(getString(R.string.vote_colors), listOf(), listOf()),
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
          ).asCustomItem(
            {
              val upvoteColor = preferences.downvoteColor
              val downvoteColor = preferences.upvoteColor

              preferences.upvoteColor = upvoteColor
              preferences.downvoteColor = downvoteColor

              postAndCommentViewBuilder.onPreferencesChanged()
            }
          )
        )
      ),
      SettingModelItem.SubgroupItem(
        SubgroupItem(getString(R.string.misc), listOf(), listOf()),
        listOf(
          settings.transparentNotificationBar.asOnOffSwitch(
            { preferences.transparentNotificationBar },
            {
              preferences.transparentNotificationBar = it
              getMainActivity()?.onPreferencesChanged()
            },
          )
        )
      ),
    )
  }
}