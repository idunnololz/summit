package com.idunnololz.summit.preview

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentVideoViewerBinding
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.showMoreVideoOptions
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ContentUtils
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.LoopsVideoUtils
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import com.idunnololz.summit.video.getVideoState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@AndroidEntryPoint
class VideoViewerFragment : BaseFragment<FragmentVideoViewerBinding>() {

    companion object {
        private val TAG = "VideoViewerFragment"

        private const val ORIENTATION_SLOP_DEG = 15 // in degrees

        private const val SIS_VIDEO_STATE = "SIS_VIDEO_STATE"
    }

    private val args: VideoViewerFragmentArgs by navArgs()

    private var orientationListener: OrientationEventListener? = null

    private val viewModel: VideoViewerViewModel by viewModels()

    @Inject
    lateinit var moreActionsHelper: MoreActionsHelper

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var okHttpClient: OkHttpClient

    private val playerListener: Player.Listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            binding.playerView.keepScreenOn =
                !(playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        orientationListener = object : OrientationEventListener(context) {
            @SuppressLint("SourceLockedOrientationActivity")
            override fun onOrientationChanged(orientation: Int) {
                Log.d(TAG, "Orientation: $orientation")
                // Ignore 180 since that's upside down
                if (isPortrait(orientation)) {
                    // portrait
                    activity?.apply {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                } else if (isLandscape(orientation)) {
                    // landscape
                    activity?.apply {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                } else if (isLandscapeReversed(orientation)) {
                    activity?.apply {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    }
                }
            }
        }

        if (orientationListener?.canDetectOrientation() == true) {
            if (PreferenceUtil.isVideoPlayerRotationLocked()) {
                orientationListener?.disable()
            } else {
                orientationListener?.enable()
            }
        }
    }

    private fun isLandscape(orientation: Int): Boolean =
        (orientation >= 270 - ORIENTATION_SLOP_DEG && orientation <= 270 + ORIENTATION_SLOP_DEG)

    private fun isLandscapeReversed(orientation: Int): Boolean =
        (orientation >= 90 - ORIENTATION_SLOP_DEG && orientation <= 90 + ORIENTATION_SLOP_DEG)

    private fun isPortrait(orientation: Int): Boolean =
        orientation >= 360 - ORIENTATION_SLOP_DEG && orientation <= 360 || orientation in 0..ORIENTATION_SLOP_DEG

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        requireMainActivity().apply {
            setupForFragment<VideoViewerFragment>()
        }

        setBinding(FragmentVideoViewerBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val parent = requireMainActivity()
        parent.hideSystemUI()

        parent.insets.observe(viewLifecycleOwner) {
            binding.playerView.findViewById<View>(R.id.controller_root_view).setPadding(
                it.leftInset,
                it.topInset,
                it.rightInset,
                it.bottomInset,
            )
        }

        binding.playerView.setControllerVisibilityListener(
            ControllerVisibilityListener {
                if (it == View.VISIBLE) {
                    parent.showSystemUI()
                } else {
                    parent.hideSystemUI()
                }
            },
        )

        binding.playerView.findViewById<View>(androidx.media3.ui.R.id.exo_fullscreen)
            .visibility = View.GONE

        binding.playerView.getRotateControl().apply {
            visibility = View.VISIBLE

            fun updateUi() {
                if (PreferenceUtil.isVideoPlayerRotationLocked()) {
                    setImageResource(R.drawable.ic_baseline_screen_rotation_24)
                } else {
                    setImageResource(R.drawable.ic_baseline_screen_lock_rotation_24)
                }
            }

            setOnClickListener {
                val isOrientationLocked = !PreferenceUtil.isVideoPlayerRotationLocked()
                PreferenceUtil.setVideoPlayerRotationLocked(isOrientationLocked)
                if (PreferenceUtil.isVideoPlayerRotationLocked()) {
                    orientationListener?.disable()
                } else {
                    orientationListener?.enable()
                }
                updateUi()
            }
            updateUi()
        }

        val videoState: VideoState? =
            if (savedInstanceState != null && savedInstanceState.containsKey(SIS_VIDEO_STATE)) {
                savedInstanceState.getParcelable(SIS_VIDEO_STATE)
            } else {
                args.videoState
            }

        loadVideo(context, LinkUtils.convertToHttps(args.url), args.videoType, videoState)

        binding.playerView.player?.addListener(playerListener)
    }

    override fun onDestroyView() {
        requireMainActivity().showSystemUI()
        binding.playerView.player?.removeListener(playerListener)

        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()

        orientationListener?.disable()
        orientationListener = null

        activity?.apply {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        binding.playerView.player?.let { player ->
            outState.putParcelable(SIS_VIDEO_STATE, player.getVideoState())
        }
    }

    private fun loadVideo(
        context: Context,
        url: String,
        videoType: VideoType,
        videoState: VideoState?,
    ) {
        when (videoType) {
            VideoType.Unknown -> {
                val uri = Uri.parse(url)
                if (ContentUtils.isUrlMp4(uri.path ?: "")) {
                    loadVideo(context, url, VideoType.Mp4, videoState)
                } else if (ContentUtils.isUrlWebm(uri.path ?: "")) {
                    loadVideo(context, url, VideoType.Webm, videoState)
                } else if (uri.host?.endsWith("loops.video", ignoreCase = true) == true) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                        val videoUrl = LoopsVideoUtils.extractVideoUrl(okHttpClient, url)

                        withContext(Dispatchers.Main) {
                            if (videoUrl != null) {
                                loadVideo(context, videoUrl, videoType, videoState)
                            } else {
                                showUnsupportedVideoTypeError(url)
                                (activity as? MainActivity)?.showSystemUI()
                            }
                        }
                    }
                } else if (uri.host?.endsWith("imgur.com", ignoreCase = true) == true) {
                    if (uri.path?.endsWith(".gifv", ignoreCase = true) == true) {
                        loadVideo(
                            context,
                            url.replace(".gifv", ".mp4"),
                            VideoType.Mp4,
                            videoState,
                        )
                    } else {
                        showUnsupportedVideoTypeError(url)
                        (activity as? MainActivity)?.showSystemUI()
                    }
                } else {
                    showUnsupportedVideoTypeError(url)
                    (activity as? MainActivity)?.showSystemUI()
                }
            }
            VideoType.Dash,
            VideoType.Mp4,
            VideoType.Webm,
            VideoType.Hls,
            -> {
                @Suppress("UnsafeOptInUsageError")
                binding.playerView.player = ExoPlayerManager.get(viewLifecycleOwner)
                    .getPlayerForUrl(
                        url = url,
                        videoType = videoType,
                        videoState = videoState,
                        autoPlay = preferenceManager.currentPreferences.autoPlayVideos,
                    )
                setupMoreButton(context, url, args.url, videoType)
            }
        }
    }

    private fun showUnsupportedVideoTypeError(url: String) {
        binding.loadingView.showErrorWithButton(
            getString(R.string.error_unsupported_video_type),
            getString(R.string.more_actions),
            {
                showMoreVideoOptions(url, args.url, moreActionsHelper, childFragmentManager)
            },
        )
    }

    private fun setupMoreButton(
        context: Context,
        url: String,
        originalUrl: String,
        videoType: VideoType,
    ) {
        binding.playerView.findViewById<ImageButton>(R.id.exo_more).setOnClickListener {
            showMoreVideoOptions(url, originalUrl, moreActionsHelper, childFragmentManager)
        }
    }
}
