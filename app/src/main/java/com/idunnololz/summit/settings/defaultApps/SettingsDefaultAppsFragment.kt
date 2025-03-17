package com.idunnololz.summit.settings.defaultApps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingsDefaultAppsBinding
import com.idunnololz.summit.preferences.DefaultAppPreference
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.DefaultAppsSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsDefaultAppsFragment :
    BaseFragment<FragmentSettingsDefaultAppsBinding>() {

    @Inject
    lateinit var settings: DefaultAppsSettings

    @Inject
    lateinit var preferences: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        setBinding(
            FragmentSettingsDefaultAppsBinding.inflate(inflater, container, false),
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        childFragmentManager.setFragmentResultListener(
            ChooseDefaultAppBottomSheetFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, result ->
            val result = result.getParcelableCompat<ChooseDefaultAppBottomSheetFragment.Result>(
                ChooseDefaultAppBottomSheetFragment.RESULT_KEY,
            )

            if (result != null) {
                if (result.selectedApp != null) {
                    preferences.defaultWebApp = DefaultAppPreference(
                        appName = result.selectedApp.name,
                        packageName = result.selectedApp.packageName,
                        componentName = result.componentName,
                    )
                    Utils.defaultWebApp = preferences.defaultWebApp
                    updateRendering()
                } else if (result.clear) {
                    preferences.defaultWebApp = null
                    Utils.defaultWebApp = null
                    updateRendering()
                }
            }
        }

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setupToolbar(binding.toolbar, settings.getPageName(context))
        }

        updateRendering()
    }

    private fun updateRendering() {
        val context = requireContext()
        val pm = context.packageManager

        settings.defaultWebApp.bindTo(
            binding.browserDefaultApp,
            {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://google.com")
                }
                ChooseDefaultAppBottomSheetFragment.show(childFragmentManager, intent)
            },
        )
        binding.browserDefaultApp.desc.visibility = View.VISIBLE
        binding.browserDefaultApp.desc.text =
            preferences.defaultWebApp?.let {
                try {
                    pm.getApplicationInfo(it.packageName, 0).let {
                        pm.getApplicationLabel(it)
                    }
                } catch (e: Exception) {
                    null
                } ?: it.appName ?: "Unnamed app"
            } ?: getString(R.string.none_set)
    }
}
