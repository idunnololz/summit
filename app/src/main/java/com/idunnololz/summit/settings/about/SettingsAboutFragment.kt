package com.idunnololz.summit.settings.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.DynamicColors
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingsAboutBinding
import com.idunnololz.summit.databinding.FragmentSettingsGenericBinding
import com.idunnololz.summit.lemmy.utils.showHelpAndFeedbackOptions
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.preferences.BaseTheme
import com.idunnololz.summit.preferences.ColorSchemes
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.AboutSettings
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.SearchableSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsAdapter
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.SubgroupItem
import com.idunnololz.summit.settings.asColorItem
import com.idunnololz.summit.settings.asCustomItem
import com.idunnololz.summit.settings.asOnOffSwitch
import com.idunnololz.summit.settings.asRadioGroup
import com.idunnololz.summit.settings.asSingleChoiceSelectorItem
import com.idunnololz.summit.settings.theme.ColorSchemePickerDialogFragment
import com.idunnololz.summit.settings.theme.FontPickerDialogFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.launchChangelog
import com.idunnololz.summit.util.openAppOnPlayStore
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsAboutFragment : BaseSettingsFragment() {

  @Inject
  override lateinit var settings: AboutSettings

  override fun generateData(): List<SettingModelItem> = listOf(
    settings.version.asCustomItem { launchChangelog() },
    settings.googlePlayLink.asCustomItem { openAppOnPlayStore() },
    settings.giveFeedback.asCustomItem { showHelpAndFeedbackOptions() },
    SettingModelItem.SubgroupItem(
      getString(R.string.supporters),
      listOf(
        settings.patreonSettings.asCustomItem(R.drawable.baseline_attach_money_24) {
          val directions = SettingsAboutFragmentDirections
            .actionSettingAboutFragmentToPatreonFragment()
          findNavController().navigateSafe(directions)
        },
        settings.translatorsSettings.asCustomItem(R.drawable.baseline_translate_24) {
          val directions = SettingsAboutFragmentDirections
            .actionSettingAboutFragmentToTranslatorsFragment()
          findNavController().navigateSafe(directions)
        }
      )
    )
  )
}
