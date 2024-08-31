package com.idunnololz.summit.video

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.idunnololz.summit.preview.VideoType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*

/**
 * Facilitates exoplayer reuse
 */
@SuppressLint("UnsafeOptInUsageError")
class ExoPlayerManager(
    private val context: Context,
) {

    companion object {

        private const val TAG = "ExoPlayerManager"

        private val managersMap = HashMap<LifecycleOwner, ExoPlayerManager>()

        private val players = LinkedList<ExoPlayer>()

        @ApplicationContext
        private lateinit var context: Context

        /**
         * The amount to rewind a video if we are maintaining video position and transitioning screens.
         * The idea is that the transition might be laggy so rewind slightly to account for it.
         */
        const val CONVENIENCE_REWIND_TIME_MS = 500L

        fun initialize(context: Context) {
            this.context = context.applicationContext
        }

        fun get(lifecycleOwner: LifecycleOwner): ExoPlayerManager {
            return managersMap[lifecycleOwner] ?: ExoPlayerManager(context).also { manager ->
                Log.d(TAG, "No manager found for $lifecycleOwner. Creating one...")
                managersMap[lifecycleOwner] = manager
                lifecycleOwner.lifecycle.addObserver(
                    object : DefaultLifecycleObserver {

                        override fun onResume(owner: LifecycleOwner) {
                            Log.d(TAG, "Fragment resumed. Restoring players")
                            managersMap[lifecycleOwner]?.restorePlayers()
                        }

                        override fun onPause(owner: LifecycleOwner) {
                            Log.d(TAG, "Fragment paused. Stopping players")
                            managersMap[lifecycleOwner]?.stop()
                        }

                        override fun onDestroy(owner: LifecycleOwner) {
                            Log.d(TAG, "Fragment destroyed. Cleaning up ExoPlayerManager instance")
                            // managersMap.remove(lifecycleOwner)?.destroy()
                            managersMap.remove(lifecycleOwner)?.viewDestroy()
                        }
                    },
                )
            }
        }

        fun destroyAll() {
            Log.d(TAG, "destroyAll()")
            managersMap.forEach { (_, manager) ->
                manager.destroy()
            }
            managersMap.clear()

            players.forEach {
                it.release()
            }
            players.clear()
        }
    }

    private val allocatedPlayers = LinkedList<ExoPlayer>()

    private val savedState = HashMap<ExoPlayer, ExoPlayerConfig>()

    data class ExoPlayerConfig(
        val url: String,
        val videoType: VideoType,
        val playing: Boolean = true,
        val videoState: VideoState? = null,
        val autoPlay: Boolean = true,
    )

    fun getPlayerForUrl(
        url: String,
        videoType: VideoType,
        videoState: VideoState?,
        autoPlay: Boolean = true,
    ): ExoPlayer =
        getPlayer().also {
            val config = ExoPlayerConfig(
                url = url,
                videoType = videoType,
                videoState = videoState,
                autoPlay = videoState?.playing ?: autoPlay,
                playing = videoState?.playing ?: autoPlay,
            )
            setupPlayer(it, config)
            savedState[it] = config
        }

    fun release(player: Player?) {
        player ?: return

        Log.d(TAG, "Releasing one player.")

        player.stop()
        val removed = allocatedPlayers.remove(player)

        if (!removed) {
            Log.e(TAG, "Released a player that did not belong to this manager!")
        } else {
            players.add(player as ExoPlayer)
        }
    }

    fun restorePlayers() {
        allocatedPlayers.forEach {
            val config = savedState[it] ?: return@forEach

            setupPlayer(it, config)
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping ${allocatedPlayers.size} players")
        allocatedPlayers.forEach {
            it.stop()

            val config = savedState[it] ?: return@forEach
            savedState[it] = config.copy(videoState = it.getVideoState(), playing = it.isPlaying)
        }
    }

    fun destroy() {
        Log.d(TAG, "Releasing ${allocatedPlayers.size} players")
        allocatedPlayers.forEach {
            it.release()
        }
        allocatedPlayers.clear()
    }

    fun viewDestroy() {
        Log.d(TAG, "View destroyed. Collecting ${allocatedPlayers.size} players")
        allocatedPlayers.forEach {
            it.stop()
        }
        players.addAll(allocatedPlayers)
        allocatedPlayers.clear()
    }

    fun pausePlayers() {
        allocatedPlayers.forEach {
            it.playWhenReady = false
        }
    }

    private fun getPlayer(): ExoPlayer = if (players.isNotEmpty()) {
        players.pop()
    } else {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(
                    object : Player.Listener {
                        override fun onEvents(player: Player, events: Player.Events) {
                            super.onEvents(player, events)

                            if (events.containsAny(Player.EVENT_REPEAT_MODE_CHANGED)) {
                                if (!player.isPlaying &&
                                    player.repeatMode != Player.REPEAT_MODE_OFF
                                ) {
                                    Util.handlePlayPauseButtonAction(player)
                                }
                            }
                        }
                    },
                )
            }
    }.also {
        allocatedPlayers.push(it)

        Log.d(
            TAG,
            "Allocating another player. Alloced: ${allocatedPlayers.size} " +
                "Total: ${allocatedPlayers.size + players.size}",
        )
    }

    private fun setupPlayer(player: ExoPlayer, config: ExoPlayerConfig) {
        // Create a data source factory.
        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setUserAgent("summit")
        val uri = Uri.parse(config.url)
        val mediaItem = MediaItem.fromUri(uri)
        val mediaSource = when (config.videoType) {
            VideoType.Unknown -> throw RuntimeException("Unknown video type")
            VideoType.Dash ->
                DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            VideoType.Mp4,
            VideoType.Webm,
            ->
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            VideoType.Hls ->
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
        }

        // Prepare the player with the media source.
        player.setMediaSource(mediaSource)
        player.prepare()

        val videoState = config.videoState
        if (videoState != null) {
            player.seekTo(videoState.currentTime)
            player.volume = videoState.volume
        } else {
            player.volume = 0.0f
        }

        player.playWhenReady = config.playing
    }
}
