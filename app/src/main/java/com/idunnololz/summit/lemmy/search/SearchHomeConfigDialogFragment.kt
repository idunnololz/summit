package com.idunnololz.summit.lemmy.search

import android.content.DialogInterface
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.idunnololz.summit.preferences.SearchHomeConfig
import com.idunnololz.summit.settings.BaseSettingsDialogFragment
import com.idunnololz.summit.settings.SearchHomeSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchHomeConfigDialogFragment : BaseSettingsDialogFragment() {

  companion object {
    const val REQUEST_KEY = "SearchHomeConfigDialogFragment_req"

    fun show(fragmentManager: FragmentManager) {
      SearchHomeConfigDialogFragment()
        .show(fragmentManager, "SearchHomeConfigFragment")
    }
  }

  @Inject
  override lateinit var settings: SearchHomeSettings

  override fun generateData(): List<SettingModelItem> {
    var searchHomeConfig = preferences.searchHomeConfig

    fun updateConfig(config: SearchHomeConfig) {
      preferences.searchHomeConfig = config
      searchHomeConfig = config
    }

    return listOf(
      settings.searchSuggestions.asOnOffSwitch(
        { searchHomeConfig.showSearchSuggestions },
        {
          updateConfig(searchHomeConfig.copy(showSearchSuggestions = it))
        },
      ),
      settings.subscribedCommunities.asOnOffSwitch(
        { searchHomeConfig.showSubscribedCommunities },
        {
          updateConfig(searchHomeConfig.copy(showSubscribedCommunities = it))
        },
      ),
      settings.topCommunities.asOnOffSwitch(
        { searchHomeConfig.showTopCommunity7DaysSuggestions },
        {
          updateConfig(searchHomeConfig.copy(showTopCommunity7DaysSuggestions = it))
        },
      ),
      settings.trendingCommunities.asOnOffSwitch(
        { searchHomeConfig.showTrendingCommunitySuggestions },
        {
          updateConfig(searchHomeConfig.copy(showTrendingCommunitySuggestions = it))
        },
      ),
      settings.risingCommunities.asOnOffSwitch(
        { searchHomeConfig.showRisingCommunitySuggestions },
        {
          updateConfig(searchHomeConfig.copy(showRisingCommunitySuggestions = it))
        },
      ),
    )
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)

    setFragmentResult(
      requestKey = REQUEST_KEY,
      result = bundleOf(),
    )
  }
}
