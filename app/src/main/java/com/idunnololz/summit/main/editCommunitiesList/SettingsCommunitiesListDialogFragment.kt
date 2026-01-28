package com.idunnololz.summit.main.editCommunitiesList

import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.settings.BaseSettingsDialogFragment
import com.idunnololz.summit.settings.CommunitySelectorCommunitiesListSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asRadioGroup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsCommunitiesListDialogFragment :
  BaseSettingsDialogFragment() {

  companion object {
    fun show(fragmentManager: FragmentManager) {
      SettingsCommunitiesListDialogFragment().show(fragmentManager, "EditCommunitiesListDialogFragment")
    }
  }

  @Inject
  override lateinit var settings: CommunitySelectorCommunitiesListSettings

  override fun generateData(): List<SettingModelItem> =
    listOf(
      settings.communitySelectorCommunitiesList.asRadioGroup(
        { preferences.communitySelectorCommunitiesList },
        { preferences.communitySelectorCommunitiesList = it }
      )
    )

}

