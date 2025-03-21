package com.idunnololz.summit.settings.backupAndRestore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.FragmentViewCurrentSettingsBinding
import com.idunnololz.summit.db.MainDatabase.Companion.DATABASE_NAME
import com.idunnololz.summit.db.preview.TableDetailsDialogFragment
import com.idunnololz.summit.settings.AllSettings
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewStartAndEndByPadding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ViewCurrentSettingsFragment :
    BaseFragment<FragmentViewCurrentSettingsBinding>(),
    OldAlertDialogFragment.AlertDialogFragmentListener {

    private val viewModel: ViewCurrentSettingsViewModel by viewModels()

    @Inject
    lateinit var allSettings: AllSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentViewCurrentSettingsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            insetViewStartAndEndByPadding(viewLifecycleOwner, binding.recyclerView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
        }

        with(binding) {
            toolbar.title = getString(R.string.view_current_settings)
            toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            val adapter = SettingDataAdapter(
                context = context,
                isImporting = false,
                onSettingPreviewClick = { settingKey, settingsDataPreview ->
                    ImportSettingItemPreviewDialogFragment.show(
                        childFragmentManager,
                        settingKey,
                        settingsDataPreview.settingsPreview[settingKey] ?: "",
                        settingsDataPreview.keyToType[settingKey] ?: "?",
                    )
                },
                onTableClick = {
                    TableDetailsDialogFragment.show(
                        childFragmentManager,
                        context.getDatabasePath(DATABASE_NAME).toUri(),
                        it,
                    )
                },
                onDeleteClick = {
                    OldAlertDialogFragment.Builder()
                        .setMessage(R.string.warn_reset_setting)
                        .setPositiveButton(R.string.reset)
                        .setNegativeButton(R.string.cancel)
                        .setExtra("key", it)
                        .create()
                        .show(childFragmentManager, "reset_setting")
                },
            )
            recyclerView.adapter = adapter
            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = LinearLayoutManager(context)

            viewModel.model.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error ->
                        loadingView.showDefaultErrorMessageFor(it.error)
                    is StatefulData.Loading ->
                        loadingView.showProgressBar()
                    is StatefulData.NotStarted ->
                        loadingView.hideAll()
                    is StatefulData.Success -> {
                        loadingView.hideAll()

                        adapter.setData(it.data.settingsDataPreview)
                    }
                }
            }

            viewModel.generatePreviewFromSettingsJson(
                databaseFile = context.getDatabasePath(DATABASE_NAME),
            )
        }
    }

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
        val context = requireContext()
        if (tag == "reset_setting") {
            val settingKey = dialog.getExtra("key")
            viewModel.resetSetting(
                settingKey = settingKey,
                databaseFile = context.getDatabasePath(DATABASE_NAME),
            )
        }
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {}
}
