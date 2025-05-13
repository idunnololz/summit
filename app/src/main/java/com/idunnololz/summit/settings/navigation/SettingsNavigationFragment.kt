package com.idunnololz.summit.settings.navigation

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import com.idunnololz.summit.R
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.NavigationSettings
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import com.idunnololz.summit.settings.util.asSingleChoiceSelectorItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsNavigationFragment :
  BaseSettingsFragment() {

  @Inject
  override lateinit var settings: NavigationSettings

  private val viewModel: SettingsNavigationViewModel by viewModels()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.loadNavBarOptions()

    viewModel.navBarOptions.observe(viewLifecycleOwner) {
      refresh()
    }
  }

  override fun onPause() {
    super.onPause()

    viewModel.applyChanges()

    getMainActivity()?.onPreferencesChanged()
  }

  override fun generateData(): List<SettingModelItem> {
    val enableNavBarOptions = preferences.useCustomNavBar
    val navBarOptions = viewModel.navBarOptions.value
    fun setupNavBarOption(index: Int, setting: RadioGroupSettingItem) = setting
      .copy(
        isEnabled = enableNavBarOptions,
      )
      .asSingleChoiceSelectorItem(
        { navBarOptions?.getOrNull(index) ?: NavBarDestinations.None },
        {
          viewModel.updateDestination(index, it)
        },
      )

    return listOf(
      settings.useBottomNavBar.asOnOffSwitch(
        { preferences.useBottomNavBar },
        {
          preferences.useBottomNavBar = it
        },
      ),
      settings.useCustomNavBar.asOnOffSwitch(
        { preferences.useCustomNavBar },
        {
          preferences.useCustomNavBar = it
        },
      ),
      settings.navigationRailMode.asSingleChoiceSelectorItem(
        { preferences.navigationRailMode },
        {
          preferences.navigationRailMode = it
          getMainActivity()?.onPreferencesChanged()

          binding.root.doOnPreDraw {
            getMainActivity()?.hideNavBar()
          }
        },
      ),
      SettingModelItem.SubgroupItem(
        getString(R.string.navigation_options),
        listOf(
          setupNavBarOption(0, settings.navBarDest1),
          setupNavBarOption(1, settings.navBarDest2),
          setupNavBarOption(2, settings.navBarDest3),
          setupNavBarOption(3, settings.navBarDest4),
          setupNavBarOption(4, settings.navBarDest5),
        ),
      ),
      SettingModelItem.SubgroupItem(
        getString(R.string.navigation_rail_mode),
        listOf(
          settings.navRailGravity.asSingleChoiceSelectorItem(
            { preferences.navRailGravity },
            { preferences.navRailGravity = it },
          )
        )
      )
    )
  }
}
