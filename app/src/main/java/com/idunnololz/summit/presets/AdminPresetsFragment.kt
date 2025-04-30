package com.idunnololz.summit.presets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentAdminPresetsBinding
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminPresetsFragment : BaseFragment<FragmentAdminPresetsBinding>() {

  private val viewModel: AdminViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentAdminPresetsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    with(binding) {
      requireSummitActivity().apply {
        insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)
      }

      setupToolbar(toolbar, getString(R.string.admin))

      val adapter = PresetsAdapter(
        includeHeader = false,
        onShareAPresetClick = {
          val direction = PresetsFragmentDirections
            .actionPresetsFragmentToGeneratePresetFragment()
          findNavController().navigateSafe(direction)
        },
      )

      swipeRefreshLayout.setOnRefreshListener {
        viewModel.loadData(force = true)
      }

      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.setHasFixedSize(true)
      recyclerView.adapter = adapter

      viewModel.loadData()

      loadingView.setOnRefreshClickListener {
        viewModel.loadData(force = true)
      }

      viewModel.model.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            swipeRefreshLayout.isRefreshing = false
            loadingView.showDefaultErrorMessageFor(it.error)
          }
          is StatefulData.Loading -> loadingView.showProgressBar()
          is StatefulData.NotStarted -> {}
          is StatefulData.Success -> {
            swipeRefreshLayout.isRefreshing = false
            loadingView.hideAll()

            adapter.setData(it.data.presets)
          }
        }
      }
    }
  }
}
