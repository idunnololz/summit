package com.idunnololz.summit.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingsBinding
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.summitCommunityPage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {
    private val args by navArgs<SettingsFragmentArgs>()

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var mainSettings: MainSettings

    private var adapter: SettingItemsAdapter? = null

    private var handledLink = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()

            insetViewExceptTopAutomaticallyByMargins(viewLifecycleOwner, binding.recyclerView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.collapsingToolbarLayout)

            setSupportActionBar(binding.searchBar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = context.getString(R.string.settings)
            supportActionBar?.hide()

            binding.searchBar.updateLayoutParams<AppBarLayout.LayoutParams> {
                scrollFlags = 0
            }
        }

        with(binding) {
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)

            if (adapter == null) {
                adapter = SettingItemsAdapter(
                    context = context,
                    onSettingClick = {
                        when (it) {
                            mainSettings.settingViewType.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingViewTypeFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingTheme.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingThemeFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingPostAndComment.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingPostAndCommentsFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingLemmyWeb.id -> {
                                launchWebSettings()
                                true
                            }
                            mainSettings.settingGestures.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingGesturesFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingHistory.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingHistoryFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingCache.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingCacheFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingPostList.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingsContentFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.commentListSettings.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingCommentListFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingHiddenPosts.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingHiddenPostsFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingAbout.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingAboutFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingSummitCommunity.id -> {
                                val fm = parentFragmentManager
                                for (i in 0 until fm.backStackEntryCount) {
                                    fm.popBackStack()
                                }
                                getMainActivity()?.launchPage(summitCommunityPage)
                                true
                            }
                            else -> false
                        }
                    },
                    childFragmentManager,
                )
            }
            recyclerView.adapter = adapter?.apply {
                this.data = settingsManager.getSettingsForMainPage()
            }
        }

        handleLinkIfNeeded()
    }

    private fun handleLinkIfNeeded() {
        val link = args.link
        if (link != null && !handledLink) {
            handledLink = true

            arguments = args.copy(link = null).toBundle()

            when (link) {
                "web" -> {
                    launchWebSettings()
                }
            }
        }
    }

    private fun launchWebSettings() {
        val directions = SettingsFragmentDirections
            .actionSettingsFragmentToSettingWebFragment()
        findNavController().navigateSafe(directions)
    }

}