package com.idunnololz.summit.settings.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingAboutBinding
import com.idunnololz.summit.databinding.FragmentSettingHistoryBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.ui.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.startFeedbackIntent
import com.idunnololz.summit.util.summitCommunityPage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingAboutFragment : BaseFragment<FragmentSettingAboutBinding>() {

    @Inject
    lateinit var preferences: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
            supportActionBar?.title = context.getString(R.string.about_summit)
        }

        updateRendering()
    }

    private fun updateRendering() {
        BasicSettingItem(
            null,
            getString(R.string.build_version, BuildConfig.VERSION_NAME),
            getString(R.string.version_code, BuildConfig.VERSION_CODE.toString())
        ).bindTo(binding.version, {})

        BasicSettingItem(
            null,
            getString(R.string.view_on_the_play_store),
            null
        ).bindTo(binding.playStoreListing) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.idunnololz.summit")
                    )
                )
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.idunnololz.summit")
                    )
                )
            }
        }

        BasicSettingItem(
            null,
            getString(R.string.give_feedback),
            getString(R.string.give_feedback_desc)
        ).bindTo(binding.playStoreListing) {
            val bottomMenu = BottomMenu(requireContext()).apply {
                setTitle(R.string.give_feedback)
                addItemWithIcon(R.id.summit_community, R.string.through_the_community, R.drawable.ic_logo_mono_24)
                addItemWithIcon(R.id.email, R.string.by_email, R.drawable.baseline_email_24)

                setOnMenuItemClickListener {
                    when(it.id) {
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
    }
}