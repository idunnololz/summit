package com.idunnololz.summit.settings.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.databinding.FragmentSettingsAboutBinding
import com.idunnololz.summit.lemmy.utils.showHelpAndFeedbackOptions
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.AboutSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.launchChangelog
import com.idunnololz.summit.util.openAppOnPlayStore
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsAboutFragment : BaseFragment<FragmentSettingsAboutBinding>() {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var aboutSettings: AboutSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsAboutBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setupToolbar(binding.toolbar, aboutSettings.getPageName(context))
        }

        updateRendering()
    }

    private fun updateRendering() {
        aboutSettings.version.bindTo(binding.version) {
            launchChangelog()
        }

        aboutSettings.googlePlayLink.bindTo(binding.playStoreListing) {
            openAppOnPlayStore()
        }

        aboutSettings.giveFeedback.bindTo(binding.giveFeedback) {
            showHelpAndFeedbackOptions()
        }

        aboutSettings.patreonSettings.bindTo(binding.patreon) {
            val directions = SettingsAboutFragmentDirections
                .actionSettingAboutFragmentToPatreonFragment()
            findNavController().navigateSafe(directions)
        }
        aboutSettings.translatorsSettings.bindTo(binding.translators) {
            val directions = SettingsAboutFragmentDirections
                .actionSettingAboutFragmentToTranslatorsFragment()
            findNavController().navigateSafe(directions)
        }
    }
}
