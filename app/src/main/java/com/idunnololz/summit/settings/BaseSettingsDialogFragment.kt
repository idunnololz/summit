package com.idunnololz.summit.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.databinding.DialogFragmentBaseSettingsBinding
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.setupToolbar
import javax.inject.Inject

abstract class BaseSettingsDialogFragment :
  BaseDialogFragment<DialogFragmentBaseSettingsBinding>() {

  @Inject
  lateinit var preferences: Preferences

  @Inject
  lateinit var globalStateStorage: GlobalStateStorage

  abstract val settings: BaseSettings

  override fun onStart() {
    super.onStart()

    setSizeDynamically(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentBaseSettingsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    requireSummitActivity().apply {
      setupToolbar(binding.toolbar, settings.getPageName(context))
    }

    updateRendering()
  }

  private fun updateRendering() {
    val context = requireContext()

    with(binding) {
      val adapter = SettingsAdapter(
        context = context,
        globalStateStorage = globalStateStorage,
        useFooter = false,
        getSummitActivity = { requireSummitActivity() },
        onValueChanged = { refresh() },
        onLinkClick = { url, text, linkContext ->
          onLinkClick(url, text, linkContext)
        },
      )
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.setHasFixedSize(false)
      recyclerView.adapter = adapter

      val data = generateData()

      adapter.setData(data)
    }
  }

  fun refresh() {
    if (!isBindingAvailable()) return

    (binding.recyclerView.adapter as? SettingsAdapter)?.setData(generateData())
  }

  abstract fun generateData(): List<SettingModelItem>
}
