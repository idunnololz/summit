package com.idunnololz.summit.settings.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingAboutBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.AboutSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.launchChangelog
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.startFeedbackIntent
import com.idunnololz.summit.util.summitCommunityPage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingAboutFragment : BaseFragment<FragmentSettingAboutBinding>() {

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

        setBinding(FragmentSettingAboutBinding.inflate(inflater, container, false))

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
            supportActionBar?.title = aboutSettings.getPageName(context)
        }

        updateRendering()
    }

    private fun updateRendering() {
        aboutSettings.version.bindTo(binding.version) {
            launchChangelog()
        }

        aboutSettings.googlePlayLink.bindTo(binding.playStoreListing) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.idunnololz.summit"),
                    ),
                )
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://play.google.com/store/apps/details?id=com.idunnololz.summit",
                        ),
                    ),
                )
            }
        }

        aboutSettings.giveFeedback.bindTo(binding.giveFeedback) {
            val bottomMenu = BottomMenu(requireContext()).apply {
                setTitle(R.string.give_feedback)
                addItemWithIcon(
                    R.id.summit_community,
                    R.string.through_the_community,
                    R.drawable.ic_logo_mono_24,
                )
                addItemWithIcon(R.id.email, R.string.by_email, R.drawable.baseline_email_24)

                setOnMenuItemClickListener {
                    when (it.id) {
                        R.id.email -> startFeedbackIntent(requireContext())
                        R.id.summit_community -> {
                            val fm = parentFragmentManager
                            for (i in 0 until fm.backStackEntryCount) {
                                fm.popBackStack()
                            }
                            getMainActivity()?.launchPage(summitCommunityPage)
                        }
                    }
                }
            }

            getMainActivity()?.showBottomMenu(bottomMenu)
        }

        aboutSettings.patreonSettings.bindTo(binding.patreon) {
            val directions = SettingAboutFragmentDirections
                .actionSettingAboutFragmentToPatreonFragment()
            findNavController().navigateSafe(directions)
        }
        aboutSettings.translatorsSettings.bindTo(binding.translators) {
            val directions = SettingAboutFragmentDirections
                .actionSettingAboutFragmentToTranslatorsFragment()
            findNavController().navigateSafe(directions)
        }
    }
}
