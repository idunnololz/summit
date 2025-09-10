package com.idunnololz.summit.settings.importAndExport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentManageInternalSettingsBackupsBinding
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ManageInternalSettingsBackupsFragment :
  BaseFragment<DialogFragmentManageInternalSettingsBackupsBinding>() {

  private val viewModel: ManageInternalSettingsBackupsViewModel by viewModels()

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(
      DialogFragmentManageInternalSettingsBackupsBinding.inflate(
        inflater,
        container,
        false,
      ),
    )

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    requireSummitActivity().apply {
      insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)

      setSupportActionBar(binding.toolbar)

      supportActionBar?.setDisplayShowHomeEnabled(true)
      supportActionBar?.setDisplayHomeAsUpEnabled(true)
      supportActionBar?.title = getString(R.string.manage_internal_settings_backups)
    }

    with(binding) {
      val adapter = BackupsAdapter(
        context = context,
        onBackupClick = { backupInfo ->
          val bottomMenu = BottomMenu(context).apply {
            addItemWithIcon(
              id = R.id.delete,
              title = R.string.delete_backup,
              icon = R.drawable.baseline_delete_24,
            )

            setOnMenuItemClickListener {
              when (it.id) {
                R.id.delete -> {
                  viewModel.deleteBackup(backupInfo)
                }
              }
            }
          }

          getMainActivity()?.showBottomMenu(bottomMenu)
        },
      )

      recyclerView.setup(animationsHelper)
      recyclerView.setHasFixedSize(true)
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.adapter = adapter

      viewModel.backupsInfo.observe(viewLifecycleOwner) {
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
            if (it.data.isEmpty()) {
              loadingView.showErrorText(
                R.string.there_doesnt_seem_to_be_anything_here,
              )
            } else {
              loadingView.hideAll()
              adapter.backupData = it.data
            }
          }
        }
      }
    }
  }
}
