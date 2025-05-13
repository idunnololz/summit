package com.idunnololz.summit.settings.defaultApps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.idunnololz.summit.R
import com.idunnololz.summit.preferences.DefaultAppPreference
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.DefaultAppsSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.getParcelableCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsDefaultAppsFragment :
  BaseSettingsFragment() {

  @Inject
  override lateinit var settings: DefaultAppsSettings

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    childFragmentManager.setFragmentResultListener(
      ChooseDefaultAppBottomSheetFragment.REQUEST_KEY,
      viewLifecycleOwner,
    ) { _, result ->
      val result = result.getParcelableCompat<ChooseDefaultAppBottomSheetFragment.Result>(
        ChooseDefaultAppBottomSheetFragment.RESULT_KEY,
      )

      if (result != null) {
        if (result.selectedApp != null) {
          preferences.defaultWebApp = DefaultAppPreference(
            appName = result.selectedApp.name,
            packageName = result.selectedApp.packageName,
            componentName = result.componentName,
          )
          Utils.defaultWebApp = preferences.defaultWebApp
          refresh()
        } else if (result.clear) {
          preferences.defaultWebApp = null
          Utils.defaultWebApp = null
          refresh()
        }
      }
    }
  }

  override fun generateData(): List<SettingModelItem> {
    val context = requireContext()
    val pm = context.packageManager

    return listOf(
      settings.defaultWebApp.copy(
        description = preferences.defaultWebApp?.let {
          try {
            pm.getApplicationInfo(it.packageName, 0).let {
              pm.getApplicationLabel(it)
            }.toString()
          } catch (e: Exception) {
            null
          } ?: it.appName ?: "Unnamed app"
        } ?: getString(R.string.none_set),
      ).asCustomItem {
        val intent = Intent(Intent.ACTION_VIEW).apply {
          data = Uri.parse("https://google.com")
        }
        ChooseDefaultAppBottomSheetFragment.show(childFragmentManager, intent)
      },
    )
  }
}
