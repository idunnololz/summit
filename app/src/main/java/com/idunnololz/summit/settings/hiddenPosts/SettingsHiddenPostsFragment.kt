package com.idunnololz.summit.settings.hiddenPosts

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.hidePosts.HiddenPostsManager
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.HiddenPostsSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.asCustomItem
import com.idunnololz.summit.settings.asOnOffSwitch
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.ext.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class SettingsHiddenPostsFragment :
  BaseSettingsFragment() {

  @Inject
  lateinit var hiddenPostsManager: HiddenPostsManager

  @Inject
  override lateinit var settings: HiddenPostsSettings

  private var count: Long? = null

  val resetHiddenPostsDialogLauncher = newAlertDialogLauncher("reset") {
    if (it.isOk) {
      lifecycleScope.launch {
        val count = withContext(Dispatchers.Default) {
          hiddenPostsManager.getHiddenPostsCount()
        }
        hiddenPostsManager.clearHiddenPosts()

        Snackbar
          .make(
            binding.coordinatorLayout,
            getString(R.string.removed_hidden_posts_format, count.toString()),
            Snackbar.LENGTH_LONG,
          )
          .show()
      }
    }
  }

  override fun generateData(): List<SettingModelItem> {
    val defaultDecimalFormat = PrettyPrintUtils.defaultDecimalFormat

    lifecycleScope.launch {
      if (count == null) {
        count = hiddenPostsManager.getHiddenPostsCount()
        refresh()
      }
    }

    return listOf(
      settings.enableHiddenPosts.asOnOffSwitch(
        { preferences.isHiddenPostsEnabled },
        {
          preferences.isHiddenPostsEnabled = it
        },
      ),
      settings.hiddenPostsCount.copy(
        description = getString(
          R.string.hidden_posts_format,
          defaultDecimalFormat.format(count ?: 0),
          defaultDecimalFormat.format(hiddenPostsManager.hiddenPostsLimit),
        ),
      ).asCustomItem { },
      settings.resetHiddenPosts.asCustomItem {
        lifecycleScope.launch {
          val count = withContext(Dispatchers.Default) {
            hiddenPostsManager.getHiddenPostsCount()
          }

          withContext(Dispatchers.Main) {
            resetHiddenPostsDialogLauncher.launchDialog {
              titleResId = R.string.reset_hidden_posts_confirm_title
              message = getString(
                R.string.reset_hidden_posts_confirm_desc_format,
                count.toString(),
              )
              positionButtonResId = R.string.yes
              negativeButtonResId = R.string.no
            }
          }
        }
      },
      settings.viewHiddenPosts.asCustomItem {
        val directions = SettingsHiddenPostsFragmentDirections
          .actionSettingHiddenPostsFragmentToHiddenPostsFragment()
        findNavController().navigateSafe(directions)
      },
    )
  }
}
