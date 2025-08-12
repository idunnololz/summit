package com.idunnololz.summit.settings.about

import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.utils.showHelpAndFeedbackOptions
import com.idunnololz.summit.settings.AboutSettings
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.SubgroupItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.util.Utils
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
    settings.version.asCustomItem { launchChangelog() }
      .copy(onLongClick = {
        val toCopy = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        Utils.copyToClipboard(
          context = requireContext(),
          toCopy = toCopy,
        )
        Toast.makeText(
          requireContext(),
          R.string.version_code_copied_to_clipboard,
          Toast.LENGTH_SHORT,
        ).show()
      }),
    settings.googlePlayLink.asCustomItem { openAppOnPlayStore() },
    settings.giveFeedback.asCustomItem { showHelpAndFeedbackOptions() },
    SettingModelItem.SubgroupItem(
      title = getString(R.string.supporters),
      settings = listOf(
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
