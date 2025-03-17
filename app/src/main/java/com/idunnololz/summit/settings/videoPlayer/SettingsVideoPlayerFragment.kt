package com.idunnololz.summit.settings.videoPlayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.databinding.FragmentSettingsVideoPlayerBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.VideoPlayerSettings
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsVideoPlayerFragment :
    BaseFragment<FragmentSettingsVideoPlayerBinding>() {

    @Inject
    lateinit var settings: VideoPlayerSettings

    @Inject
    lateinit var preferences: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        setBinding(
            FragmentSettingsVideoPlayerBinding.inflate(inflater, container, false),
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setupToolbar(binding.toolbar, settings.getPageName(context))
        }

        updateRendering()
    }

    private fun updateRendering() {
        settings.autoPlayVideos.bindTo(
            binding.autoPlayVideos,
            { preferences.autoPlayVideos },
            {
                preferences.autoPlayVideos = it
            },
        )
        settings.inlineVideoVolume.bindTo(
            binding.inlineVideoVolume,
            { preferences.inlineVideoDefaultVolume },
            { preferences.inlineVideoDefaultVolume = it },
        )
    }
}
