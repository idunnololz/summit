package com.idunnololz.summit.localTracking.screen

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentLocalStatsBinding
import com.idunnololz.summit.lemmy.toUri
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocalStatsFragment : BaseFragment<FragmentLocalStatsBinding>() {

  private val viewModel: LocalStatsViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentLocalStatsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      requireSummitActivity().apply {
        insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
        insetViewAutomaticallyByPaddingAndNavUi(
          viewLifecycleOwner,
          root,
          applyTopInset = false,
        )

        setupToolbar(toolbar, getString(R.string.local_account_stats))
      }

      viewModel.loadData(force = false)

      viewModel.data.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            loadingView.showDefaultErrorMessageFor(it.error)
          }
          is StatefulData.Loading -> {
            loadingView.showProgressBar()
          }
          is StatefulData.NotStarted -> {
            loadingView.hideAll()
          }
          is StatefulData.Success -> {
            loadingView.hideAll()

            stats.movementMethod = ScrollingMovementMethod()
            stats.setText(
              buildString {
                append("Total events: ")
                appendLine(it.data.events)

                appendLine()
                appendLine("Most visited communities: ")
                it.data.mostVisitedCommunities.forEach {
                  append(it.key?.getName(context))
                  append(" - ")
                  appendLine(it.value)
                }


                appendLine()
                appendLine("Users you interact with the most: ")
                it.data.userInteractions.forEach {
                  append(it.first?.name)
                  append(" - ")
                  appendLine(it.second)
                }


                appendLine()
                appendLine("Favorite communities: ")
                it.data.favoriteCommunities.forEach {
                  append(it.key?.getName(context))
                  append(" - ")
                  appendLine(it.value)
                }


              }
            )
          }
        }
      }
    }
  }
}