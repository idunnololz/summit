package com.idunnololz.summit.settings.people

import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.MiscSettings
import com.idunnololz.summit.settings.PeopleSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.asOnOffSwitch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsPeopleFragment :
  BaseSettingsFragment(),
  SettingValueUpdateCallback {

  @Inject
  override lateinit var settings: PeopleSettings

  override fun generateData(): List<SettingModelItem> {
    val context = requireContext()

    return listOf(
      settings.preferUserDisplayNames.asOnOffSwitch(
        { preferences.preferUserDisplayName },
        { preferences.preferUserDisplayName = it },
      ),
      settings.showNewPersonWarning.asOnOffSwitch(
        { preferences.warnNewPerson },
        { preferences.warnNewPerson = it },
      ),
      settings.showPerUserScores.asOnOffSwitch(
        { preferences.showPerUserScores },
        { preferences.showPerUserScores = it },
      ),
      settings.showBotLabel.asOnOffSwitch(
        { preferences.showBotLabel },
        { preferences.showBotLabel = it },
      ),
      settings.showPronounsIfAvailable.asOnOffSwitch(
        { preferences.showPronounsIfAvailable },
        { preferences.showPronounsIfAvailable = it },
      ),
    )
  }

  override fun updateValue(key: Int, value: Any?) {

  }
}
