package com.idunnololz.summit.settings.perCommunity

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.ColorSettingItem
import com.idunnololz.summit.settings.DescriptionSettingItem
import com.idunnololz.summit.settings.ImageValueSettingItem
import com.idunnololz.summit.settings.OnOffSettingItem
import com.idunnololz.summit.settings.PerCommunitySettings
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.settings.SettingItem
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.SliderSettingItem
import com.idunnololz.summit.settings.SubgroupItem
import com.idunnololz.summit.settings.TextOnlySettingItem
import com.idunnololz.summit.settings.TextValueSettingItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsPerCommunityFragment : BaseSettingsFragment() {

  private val viewModel: SettingsPerCommunityViewModel by viewModels()

  @Inject
  override lateinit var settings: PerCommunitySettings

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.data.observe(viewLifecycleOwner) {
      refresh()
    }

    viewModel.loadData(settings)
  }

  private fun List<SettingItem>.toSettingModelItems(
    settingValues: Map<Int, Any?>,
  ): List<SettingModelItem> {
    val result = mutableListOf<SettingModelItem>()
    this.forEach { settingItem ->
      when (settingItem) {
        is BasicSettingItem -> {
          result += settingItem.asCustomItem {
            viewModel.onSettingClick(settings, settingItem)
          }
        }
        is ColorSettingItem -> TODO()
        is DescriptionSettingItem -> TODO()
        is ImageValueSettingItem -> TODO()
        is OnOffSettingItem -> {
          result += settingItem.asOnOffSwitch(
            { settingValues[settingItem.id] as? Boolean ?: false },
            { viewModel.onSettingClick(settings, settingItem) },
          )
        }
        is RadioGroupSettingItem -> TODO()
        is SliderSettingItem -> TODO()
        is SubgroupItem -> {
          result += SettingModelItem.SubgroupItem(
            settingItem.title,
            settingItem.settings.toSettingModelItems(settingValues),
          )
        }
        is TextOnlySettingItem -> TODO()
        is TextValueSettingItem -> TODO()
      }
    }
    return result
  }

  override fun generateData(): List<SettingModelItem> {
    val settingItems = mutableListOf<SettingModelItem>()
    val data = viewModel.data.valueOrNull ?: return settingItems

    settingItems.addAll(data.settingItems.toSettingModelItems(data.settingValues))

    return settingItems
  }
}
