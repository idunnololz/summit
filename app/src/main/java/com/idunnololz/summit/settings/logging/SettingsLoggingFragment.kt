package com.idunnololz.summit.settings.logging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingsLoggingBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.LoggingSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsLoggingFragment :
    BaseFragment<FragmentSettingsLoggingBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var settings: LoggingSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsLoggingBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = settings.getPageName(context)
        }

        updateRendering()
    }

    private fun updateRendering() {
        if (!isBindingAvailable()) {
            return
        }

        val context = requireContext()

        settings.useFirebase.bindTo(
            b = binding.useFirebase,
            { preferences.useFirebase },
            {
                AlertDialogFragment.Builder()
                    .setTitle(R.string.app_restart_required_by_setting)
                    .setMessage(R.string.app_restart_required_by_setting_desc)
                    .setPositiveButton(R.string.restart_app)
                    .setNegativeButton(R.string.cancel)
                    .createAndShow(
                        childFragmentManager,
                        if (it) {
                            "enableFirebase"
                        } else {
                            "disableFirebase"
                        },
                    )
            },
        )
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            "enableFirebase" -> {
                preferences.useFirebase = true
                ProcessPhoenix.triggerRebirth(requireContext())
            }
            "disableFirebase" -> {
                preferences.useFirebase = false
                ProcessPhoenix.triggerRebirth(requireContext())
            }
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            "enableFirebase",
            "disableFirebase",
            -> {
                updateRendering()
            }
        }
    }
}
