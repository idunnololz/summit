package com.idunnololz.summit.preview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentVideoViewerBinding
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.util.*
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import com.idunnololz.summit.video.getVideoState
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException

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

        parent.windowInsets.observe(viewLifecycleOwner) {
            binding.playerView.findViewById<View>(R.id.controller_root_view).setPadding(
                it.left,
                it.top,
                it.right,
                it.bottom,
            )
        }

        viewModel.downloadVideoResult.observe(this) {
            when (it) {
                is StatefulData.NotStarted -> {}
                is StatefulData.Error -> {
                    FirebaseCrashlytics.getInstance().recordException(it.error)
                    Snackbar.make(parent.getSnackbarContainer(), R.string.error_downloading_image, Snackbar.LENGTH_LONG)
                        .show()
                }
                is StatefulData.Loading -> {}
                is StatefulData.Success -> {
                    try {
                        val downloadResult = it.data
                        val uri = downloadResult.uri
                        val mimeType = downloadResult.mimeType

                        val snackbarMsg = getString(R.string.video_saved_format, downloadResult.uri)
                        Snackbar.make(
                            parent.getSnackbarContainer(),
                            snackbarMsg,
                            Snackbar.LENGTH_LONG,
                        ).setAction(R.string.view) {
                            Utils.safeLaunchExternalIntentWithErrorDialog(
                                context,
                                childFragmentManager,
                                Intent(Intent.ACTION_VIEW).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    setDataAndType(uri, mimeType)
                                },
                            )
                        }.show()
                    } catch (e: IOException) { /* do nothing */
                    }
                }
            }
        }

        binding.playerView.setControllerVisibilityListener(
            ControllerVisibilityListener {
                if (it == View.VISIBLE) {
                    parent.showSystemUI(animate = true)
                } else {
                    parent.hideSystemUI()
                }
            },
        )

        binding.playerView.findViewById<View>(androidx.media3.ui.R.id.exo_fullscreen).visibility = View.GONE

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
        requireMainActivity().showSystemUI(animate = true)
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
            VideoType.UNKNOWN -> {
                val uri = Uri.parse(url)
                if (uri.host?.endsWith("imgur.com", ignoreCase = true) == true) {
                    if (uri.path?.endsWith(".gifv", ignoreCase = true) == true) {
                        loadVideo(
                            context,
                            url.replace(".gifv", ".mp4"),
                            VideoType.MP4,
                            videoState,
                        )
                    } else {
                        binding.loadingView.showErrorText(R.string.unsupported_video_type)
                        (activity as? MainActivity)?.showSystemUI(animate = true)
                    }
                } else if (uri.path?.endsWith("mp4", ignoreCase = true) == true) {
                    loadVideo(context, url, VideoType.MP4, videoState)
                } else {
                    binding.loadingView.showErrorText(R.string.unsupported_video_type)
                    (activity as? MainActivity)?.showSystemUI(animate = true)
                }
            }
            VideoType.DASH -> {
                @Suppress("UnsafeOptInUsageError")
                binding.playerView.player = ExoPlayerManager.get(viewLifecycleOwner)
                    .getPlayerForUrl(url, videoType, videoState)
                setupMoreButton(context, url, videoType)
            }
            VideoType.MP4 -> {
                @Suppress("UnsafeOptInUsageError")
                binding.playerView.player = ExoPlayerManager.get(viewLifecycleOwner)
                    .getPlayerForUrl(url, videoType, videoState)
                setupMoreButton(context, url, videoType)
            }
        }
    }

    private fun setupMoreButton(context: Context, url: String, videoType: VideoType) {
        binding.playerView.findViewById<ImageButton>(R.id.exo_more).setOnClickListener {
            PopupMenu(context, it).apply {
                inflate(R.menu.video_menu)

                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.save -> {
                            viewModel.downloadVideo(requireContext(), url)
//                            AlertDialogFragment.Builder()
//                                .setMessage(R.string.coming_soon)
//                                .createAndShow(childFragmentManager, "asdf")
                            true
                        }
                        else -> false
                    }
                }

                show()
            }
        }
    }
}
