package com.idunnololz.summit.settings.perCommunity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingsPerCommunityBinding
import com.idunnololz.summit.settings.PerCommunitySettings
import com.idunnololz.summit.settings.SettingItemsAdapter
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsPerCommunityFragment : BaseFragment<FragmentSettingsPerCommunityBinding>() {

  private val viewModel: SettingsPerCommunityViewModel by viewModels()

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var settings: PerCommunitySettings

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentSettingsPerCommunityBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    requireSummitActivity().apply {
      setupForFragment<SettingsFragment>()
      insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
      insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.recyclerView)

      setSupportActionBar(binding.toolbar)

      supportActionBar?.setDisplayShowHomeEnabled(true)
      supportActionBar?.setDisplayHomeAsUpEnabled(true)
      supportActionBar?.title = context.getString(R.string.per_community_settings)
    }

    with(binding) {
      val adapter = SettingItemsAdapter(
        context = context,
        onSettingClick = {
          viewModel.onSettingClick(settings, it)
          true
        },
        fragmentManager = childFragmentManager,
      )

      viewModel.data.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> loadingView.showDefaultErrorMessageFor(it.error)
          is StatefulData.Loading -> loadingView.showProgressBar()
          is StatefulData.NotStarted -> loadingView.hideAll()
          is StatefulData.Success -> {
            loadingView.hideAll()

            adapter.setData(it.data.settingItems)
            adapter.defaultSettingValues = it.data.settingValues
          }
        }
      }

      recyclerView.setup(animationsHelper)
      recyclerView.adapter = adapter
      recyclerView.setHasFixedSize(true)
      recyclerView.layoutManager = LinearLayoutManager(context)
    }

    viewModel.loadData(settings)
  }
}
