package com.idunnololz.summit.presets

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuItemCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentPresetsBinding
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PresetsFragment : BaseFragment<FragmentPresetsBinding>() {

  private val viewModel: PresetsViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentPresetsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      requireSummitActivity().apply {
        insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)
      }

      setupToolbar(toolbar, getString(R.string.presets))

      toolbar.addMenuProvider(
        object : MenuProvider {
          override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            if (BuildConfig.SUMMIT_JWT.isNotBlank()) {
              val color = context.getColorFromAttribute(
                androidx.appcompat.R.attr.colorControlNormal,
              )
              menu.add(0, R.id.admin, 0, R.string.admin)
                .apply {
                  setIcon(R.drawable.outline_shield_24)
                  setShowAsAction(SHOW_AS_ACTION_IF_ROOM)
                  MenuItemCompat.setIconTintList(
                    this,
                    ColorStateList.valueOf(color),
                  )
                }
            }
          }

          override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
              R.id.admin -> {
                val directions = PresetsFragmentDirections
                  .actionPresetsFragmentToAdminPresetsFragment()
                findNavController().navigateSafe(directions)
              }
            }
            return true
          }
        },
      )

      val adapter = PresetsAdapter(
        includeHeader = true,
        onShareAPresetClick = {
          val direction = PresetsFragmentDirections
            .actionPresetsFragmentToGeneratePresetFragment()
          findNavController().navigateSafe(direction)
        },
      )

      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.setHasFixedSize(true)
      recyclerView.adapter = adapter

      viewModel.loadData()

      loadingView.setOnRefreshClickListener {
        viewModel.loadData(force = true)
      }

      viewModel.model.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> loadingView.showDefaultErrorMessageFor(it.error)
          is StatefulData.Loading -> loadingView.showProgressBar()
          is StatefulData.NotStarted -> {}
          is StatefulData.Success -> {
            loadingView.hideAll()

            adapter.setData(it.data.presets)
          }
        }
      }
    }
  }
}
