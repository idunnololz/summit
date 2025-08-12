package com.idunnololz.summit.settings.inbox

import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.InboxSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import com.idunnololz.summit.settings.util.asSingleChoiceSelectorItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsInboxFragment : BaseSettingsFragment() {

  @Inject
  override lateinit var settings: InboxSettings

  override fun generateData(): List<SettingModelItem> = listOf(
    settings.inboxAutoMarkAsRead.asOnOffSwitch(
      { preferences.inboxAutoMarkAsRead },
      { preferences.inboxAutoMarkAsRead = it },
    ),
    settings.inboxLayout.asSingleChoiceSelectorItem(
      { preferences.inboxLayout },
      { preferences.inboxLayout = it },
    ),
    settings.inboxFabAction.asSingleChoiceSelectorItem(
      { preferences.inboxFabAction },
      { preferences.inboxFabAction = it },
    ),
  )
}
