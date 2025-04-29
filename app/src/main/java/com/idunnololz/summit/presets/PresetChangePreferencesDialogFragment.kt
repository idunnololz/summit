package com.idunnololz.summit.presets

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentPresetChangePreferencesBinding
import com.idunnololz.summit.db.MainDatabase.Companion.DATABASE_NAME
import com.idunnololz.summit.db.preview.TableDetailsDialogFragment
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.importAndExport.ImportSettingItemPreviewDialogFragment
import com.idunnololz.summit.settings.importAndExport.SettingDataAdapter
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PresetChangePreferencesDialogFragment :
    BaseDialogFragment<FragmentPresetChangePreferencesBinding>(),
    FullscreenDialogFragment {

    companion object {
        fun show(fragmentManager: FragmentManager) {
            PresetChangePreferencesDialogFragment()
                .show(fragmentManager, "PresetChangePreferencesFragment")
        }
    }

    @Inject
    lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(
            FragmentPresetChangePreferencesBinding.inflate(
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

        with(binding) {
            setupToolbar(toolbar, getString(R.string.choose_preferences))

            val viewModel = (parentFragment as CreatePresetFragment).viewModel

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
                style = SettingDataAdapter.Style.Switch(
                    onSwitchClick = { key, isChecked ->
                        if (!isChecked) {
                            viewModel.excludedKeys.add(key)
                        } else {
                            viewModel.excludedKeys.remove(key)
                        }
                    },
                    isChecked = { key ->
                        !viewModel.excludedKeys.contains(key)
                    },
                ),
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

            viewModel.generatePreviewFromSettingsJson()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        (parentFragment as CreatePresetFragment).onPresetChangePreferencesDismiss()

        super.onDismiss(dialog)
    }
}
