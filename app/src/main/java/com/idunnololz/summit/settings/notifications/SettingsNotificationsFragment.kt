package com.idunnololz.summit.settings.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.viewModels
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.MainApplication
import com.idunnololz.summit.R
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.NotificationSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.SubgroupItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import com.idunnololz.summit.settings.util.asSingleChoiceSelectorItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsNotificationsFragment : BaseSettingsFragment() {

  @Inject
  override lateinit var settings: NotificationSettings

  private val viewModel: SettingsNotificationsViewModel by viewModels()

  private val requestPermissionLauncher =
    registerForActivityResult(
      ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
      setNotificationsEnabled(isGranted)

      refresh()
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.settings.observe(viewLifecycleOwner) {
      refresh()
    }
  }

  override fun generateData(): List<SettingModelItem> {
    val context = requireContext()

    val items = mutableListOf(
      settings.isNotificationsEnabled.asOnOffSwitch(
        { preferences.isNotificationsOn },
        {
          val newState = !preferences.isNotificationsOn

          if (newState &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
          ) {
            with(NotificationManagerCompat.from(context)) {
              if (ActivityCompat.checkSelfPermission(
                  context,
                  Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
              ) {
                requestPermissionLauncher.launch(
                  Manifest.permission.POST_NOTIFICATIONS,
                )

                return@with
              }

              setNotificationsEnabled(true)
            }
          } else {
            setNotificationsEnabled(newState)
          }
        },
      ),
      settings.checkInterval.asSingleChoiceSelectorItem(
        { convertCheckIntervalToOptionId(preferences.notificationsCheckIntervalMs) },
        {
          val checkInterval = convertOptionIdToCheckInterval(it)

          if (checkInterval != null) {
            preferences.notificationsCheckIntervalMs = checkInterval
            viewModel.onNotificationCheckIntervalChanged()
          }
        },
      ),
      *if (BuildConfig.DEBUG) {
        arrayOf(
          BasicSettingItem(
            null,
            "Force run notifications job",
            null,
          ).asCustomItem {
            (activity?.application as? MainApplication)?.runNotificationsUpdate()
          },
        )
      } else {
        arrayOf()
      },
    )

    items.add(
      SettingModelItem.SubgroupItem(
        SubgroupItem(
          getString(R.string.per_account_settings),
          getString(R.string.notifications_per_account_settings_desc),
          listOf(),
        ),
        viewModel.settings.value?.map { setting ->
          setting.settingItem.asOnOffSwitch(
            {
              viewModel.notificationsManager.isNotificationsEnabledForAccount(setting.account)
            },
            {
              viewModel.notificationsManager.setNotificationsEnabledForAccount(setting.account, it)
            },
          )
        } ?: listOf(),
      ),
    )

    return items
  }

  private fun setNotificationsEnabled(newState: Boolean) {
    preferences.isNotificationsOn = newState
    viewModel.onNotificationSettingsChanged()
    refresh()
  }

  private fun convertCheckIntervalToOptionId(value: Long) = when {
    value >= 1000L * 60L * 60L * 13L -> {
      R.id.unknown
    }
    value >= 1000L * 60L * 60L * 12L -> {
      R.id.refresh_interval_12_hours
    }
    value >= 1000L * 60L * 60L * 4L -> {
      R.id.refresh_interval_4_hours
    }
    value >= 1000L * 60L * 60L * 2L -> {
      R.id.refresh_interval_2_hours
    }
    value >= 1000L * 60L * 60L -> {
      R.id.refresh_interval_60_minutes
    }
    value >= 1000L * 60L * 30L -> {
      R.id.refresh_interval_30_minutes
    }
    else -> {
      R.id.refresh_interval_15_minutes
    }
  }

  private fun convertOptionIdToCheckInterval(@IdRes id: Int) = when (id) {
    R.id.refresh_interval_12_hours -> 1000L * 60L * 60L * 12L
    R.id.refresh_interval_4_hours -> 1000L * 60L * 60L * 4L
    R.id.refresh_interval_2_hours -> 1000L * 60L * 60L * 2L
    R.id.refresh_interval_60_minutes -> 1000L * 60L * 60L
    R.id.refresh_interval_30_minutes -> 1000L * 60L * 30L
    R.id.refresh_interval_15_minutes -> 1000L * 60L * 15L
    else -> null
  }
}
