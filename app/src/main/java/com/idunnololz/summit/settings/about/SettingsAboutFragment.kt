package com.idunnololz.summit.settings.about

import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.utils.showHelpAndFeedbackOptions
import com.idunnololz.summit.settings.AboutSettings
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.SubgroupItem
import com.idunnololz.summit.settings.asCustomItem
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.launchChangelog
import com.idunnololz.summit.util.openAppOnPlayStore
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
        },
      ),
    ),
  )
}
