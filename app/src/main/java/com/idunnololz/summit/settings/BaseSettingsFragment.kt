package com.idunnololz.summit.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.databinding.FragmentSettingsGenericBinding
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import javax.inject.Inject

abstract class BaseSettingsFragment : BaseFragment<FragmentSettingsGenericBinding>() {

  companion object {
    const val ARG_SETTING_NAME = "ARG_SETTING_NAME"
  }

  @Inject
  lateinit var preferences: Preferences

  @Inject
  lateinit var globalStateStorage: GlobalStateStorage

  abstract val settings: BaseSettings

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentSettingsGenericBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    requireSummitActivity().apply {
      setupForFragment<SettingsFragment>()
      insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.recyclerView)
      insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

      setupToolbar(binding.toolbar, settings.getPageName(context))
    }

    updateRendering(savedInstanceState)
  }

  private fun updateRendering(savedInstanceState: Bundle?) {
    val context = requireContext()

    with(binding) {
      val adapter = SettingsAdapter(
        context = context,
        globalStateStorage = globalStateStorage,
        useFooter = true,
        getSummitActivity = { requireSummitActivity() },
        onValueChanged = { refresh() },
        onLinkClick = { url, text, linkContext ->
          onLinkClick(url, text, linkContext)
        },
      )
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.setHasFixedSize(true)
      recyclerView.adapter = adapter

      val data = generateData()

      adapter.setData(data)

      if (savedInstanceState == null) {
        arguments?.getString(ARG_SETTING_NAME)?.let { settingName ->
          recyclerView.post {
            val index = adapter.findIndex(settingName)
            if (index >= 0) {
              (binding.recyclerView.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(index, Utils.convertDpToPixel(48f).toInt())
              adapter.highlight(settingName)
            }
          }
        }
      }
    }
  }

  fun refresh() {
    if (!isBindingAvailable()) return

    (binding.recyclerView.adapter as? SettingsAdapter)?.setData(generateData())
  }

  abstract fun generateData(): List<SettingModelItem>
}
