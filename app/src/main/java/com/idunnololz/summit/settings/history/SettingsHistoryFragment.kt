package com.idunnololz.summit.settings.history

import com.idunnololz.summit.R
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.HistorySettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsHistoryFragment : BaseSettingsFragment() {

  @Inject
  override lateinit var settings: HistorySettings

  override fun generateData(): List<SettingModelItem> = listOf(
    settings.viewHistory.asCustomItem {
      getMainActivity()?.navigateTopLevel(R.id.historyFragment)
    },
    settings.recordBrowsingHistory.asOnOffSwitch(
      { preferences.trackBrowsingHistory },
      {
        preferences.trackBrowsingHistory = it
      },
    ),
  )
}
