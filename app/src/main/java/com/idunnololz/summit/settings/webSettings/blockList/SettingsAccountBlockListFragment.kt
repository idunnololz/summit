package com.idunnololz.summit.settings.webSettings.blockList

import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.settings.AccountBlockListSettings
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.util.ext.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsAccountBlockListFragment : BaseSettingsFragment() {

  @Inject
  override lateinit var settings: AccountBlockListSettings

  override fun generateData(): List<SettingModelItem> = listOf(
    settings.blockedUsersSetting.asCustomItem {
      val directions = SettingsAccountBlockListFragmentDirections
        .actionSettingsAccountBlockListFragmentToSettingsUserBlockListFragment()
      findNavController().navigateSafe(directions)
    },
    settings.blockedCommunitiesSetting.asCustomItem {
      val directions = SettingsAccountBlockListFragmentDirections
        .actionSettingsAccountBlockListFragmentToSettingsCommunityBlockListFragment()
      findNavController().navigateSafe(directions)
    },
    settings.blockedInstancesSetting.asCustomItem {
      val directions = SettingsAccountBlockListFragmentDirections
        .actionSettingsAccountBlockListFragmentToSettingsInstanceBlockListFragment()
      findNavController().navigateSafe(directions)
    },
  )
}
