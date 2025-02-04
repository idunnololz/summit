package com.idunnololz.summit.settings.haptics

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingsHapticsBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.HapticSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.settings.util.isEnabled
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsHapticsFragment : BaseFragment<FragmentSettingsHapticsBinding>(),
    OldAlertDialogFragment.AlertDialogFragmentListener {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var settings: HapticSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsHapticsBinding.inflate(inflater, container, false))

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

        settings.haptics.bindTo(
            binding.haptics,
            { preferences.hapticsEnabled },
            {
                preferences.hapticsEnabled = it

                if (it) {
                    val hapticsEnabled =
                        Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1

                    if (!hapticsEnabled) {
                        OldAlertDialogFragment.Builder()
                            .setMessage(R.string.warn_system_haptic_feedback_disabled)
                            .setPositiveButton(R.string.settings)
                            .setNegativeButton(R.string.cancel)
                            .createAndShow(childFragmentManager, "warn_haptics_disabled")
                    }
                }

                updateRendering()
            }
        )
        settings.moreHaptics.bindTo(
            binding.moreHaptics,
            { preferences.hapticsOnActions },
            {
                preferences.hapticsOnActions = it
            }
        )

        binding.moreHaptics.isEnabled = preferences.hapticsEnabled
    }

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
        val intent = Intent(Settings.ACTION_SOUND_SETTINGS)
        startActivity(intent)
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    }
}