package com.idunnololz.summit.settings.content

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingsContentBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.OnOffSettingItem
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.cache.SettingCacheFragment
import com.idunnololz.summit.settings.ui.bindTo
import com.idunnololz.summit.util.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsContentFragment : BaseFragment<FragmentSettingsContentBinding>() {

    @Inject
    lateinit var preferences: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        requireMainActivity().apply {
            setupForFragment<SettingCacheFragment>()
        }

        setBinding(FragmentSettingsContentBinding.inflate(inflater, container, false))

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
            supportActionBar?.title = context.getString(R.string.content)
        }

        updateRendering()
    }

    private fun updateRendering() {
        OnOffSettingItem(
            getString(R.string.infinity),
            getString(R.string.no_your_limits),
        ).bindTo(
            binding.infinity,
            { preferences.infinity },
            {
                preferences.infinity = it

                updateRendering()
            }
        )
        OnOffSettingItem(
            getString(R.string.mark_posts_as_read_on_scroll),
            getString(R.string.mark_posts_as_read_on_scroll_desc),
        ).bindTo(
            binding.markPostsAsReadOnScroll,
            { preferences.markPostsAsReadOnScroll },
            {
                preferences.markPostsAsReadOnScroll = it

                updateRendering()
            }
        )
    }
}