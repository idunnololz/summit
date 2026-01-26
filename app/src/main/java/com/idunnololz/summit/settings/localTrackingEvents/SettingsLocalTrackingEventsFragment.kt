package com.idunnololz.summit.settings.localTrackingEvents

import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idunnololz.summit.R
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.LocalTrackingEventsSettings
import com.idunnololz.summit.settings.PostAndCommentsSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.postAndComments.SettingsPostAndCommentsFragmentArgs
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.settings.util.asOnOffMasterSwitch
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils.fileSizeToHumanReadableString
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class SettingsLocalTrackingEventsFragment : BaseSettingsFragment() {

  private val viewModel: SettingsLocalTrackingEventsViewModel by viewModels()

  @Inject
  override lateinit var settings: LocalTrackingEventsSettings

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.load()

    viewModel.data.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error -> {}
        is StatefulData.Loading -> {}
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {
          refresh()
        }
      }
    }
  }

  override fun generateData(): List<SettingModelItem> {
    val data = viewModel.data.valueOrNull

    return listOf(
      settings.localTrackingEvents.asOnOffMasterSwitch(
        { preferences.localTrackingEnabled },
        { preferences.localTrackingEnabled = it },
      ),
      settings.totalEventsRecorded.copy(
        description = data?.totalEvents?.let {
          PrettyPrintUtils.defaultDecimalFormat.format(it)
        } ?: "-"
      ).asCustomItem(),
      settings.sizeOnDiskRecorded.copy(
        description = data?.totalTableSize?.let {
          fileSizeToHumanReadableString(it.toDouble(), PrettyPrintUtils.defaultDecimalFormat)
        } ?: "-"
      ).asCustomItem(),
      settings.clearLocalTrackingEvents.asCustomItem {
        MaterialAlertDialogBuilder(requireContext())
          .setMessage(R.string.delete_all_local_tracking_data)
          .setPositiveButton(R.string.delete, { _, _ ->
            viewModel.deleteAllLocalTrackingData()
          })
          .setNegativeButton(R.string.cancel, { _, _ -> })
          .show()
      },
    )
  }
}