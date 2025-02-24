package com.idunnololz.summit.video

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.DirectoryHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class ExoPlayerManagerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val globalStateStorage: GlobalStateStorage,
    private val preferences: Preferences,
    private val directoryHelper: DirectoryHelper,
) {

    companion object {
        private const val TAG = "ExoPlayerManagerManager"
    }

    private val managersMap = HashMap<LifecycleOwner, ExoPlayerManager>()
    private val players = LinkedList<ExoPlayer>()
    private val downloadCache by lazy {
        directoryHelper.videoCacheDir.mkdirs()
        val downloadContentDirectory = directoryHelper.videoCacheDir

        SimpleCache(
            downloadContentDirectory,
            LeastRecentlyUsedCacheEvictor(50 * 1024 * 1024),
            StandaloneDatabaseProvider(context),
        )
    }
    private val dataSourceFactory by lazy {
        val cache = downloadCache
        val cacheSink = CacheDataSink.Factory()
            .setCache(cache)
        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setUserAgent("summit")
        val upstreamFactory =
            DefaultDataSource.Factory(context, dataSourceFactory)
        CacheDataSource.Factory()
            .setCache(cache)
            .setCacheWriteDataSinkFactory(cacheSink)
            .setCacheReadDataSourceFactory(FileDataSource.Factory())
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun get(lifecycleOwner: LifecycleOwner): ExoPlayerManager {
        return managersMap[lifecycleOwner] ?: ExoPlayerManager(
            context,
            globalStateStorage,
            players,
            preferences,
            { dataSourceFactory }
        ).also { manager ->
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

    fun onDestroyView() {

    }
}

/**
 * Facilitates exoplayer reuse
 */
@SuppressLint("UnsafeOptInUsageError")
class ExoPlayerManager(
    private val context: Context,
    private val globalStateStorage: GlobalStateStorage,
    private val players: LinkedList<ExoPlayer>,
    private val preferences: Preferences,
    private val getDataSourceFactory: () -> DataSource.Factory,
) {

    companion object {

        private const val TAG = "ExoPlayerManager"
        /**
         * The amount to rewind a video if we are maintaining video position and transitioning screens.
         * The idea is that the transition might be laggy so rewind slightly to account for it.
         */
        const val CONVENIENCE_REWIND_TIME_MS = 500L
    }

    private val allocatedPlayers = LinkedList<ExoPlayer>()

    private val savedState = HashMap<ExoPlayer, ExoPlayerConfig>()

    data class ExoPlayerConfig(
        val url: String,
        val videoType: VideoType,
        val isInline: Boolean,
        val playing: Boolean = true,
        val videoState: VideoState? = null,
        val autoPlay: Boolean = true,
    )

    fun getPlayerForUrl(
        url: String,
        videoType: VideoType,
        videoState: VideoState?,
        isInline: Boolean,
        autoPlay: Boolean = true,
    ): ExoPlayer = getPlayer(isInline).also {
        val config = ExoPlayerConfig(
            url = url,
            videoType = videoType,
            videoState = videoState,
            isInline = isInline,
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

    private fun getPlayer(inline: Boolean): ExoPlayer {
        if (!inline) {
            return newPlayer()
        }

        return if (players.isNotEmpty()) {
            players.pop()
        } else {
            newPlayer()
        }.also {
            allocatedPlayers.push(it)

            Log.d(
                TAG,
                "Allocating another player. Alloced: ${allocatedPlayers.size} " +
                    "Total: ${allocatedPlayers.size + players.size}",
            )
        }
    }

    private fun newPlayer() =
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

    private fun setupPlayer(player: ExoPlayer, config: ExoPlayerConfig) {
        // Create a data source factory.
        val uri = Uri.parse(config.url)
        val mediaItem = MediaItem.fromUri(uri)
        val mediaSource = when (config.videoType) {
            VideoType.Unknown -> throw RuntimeException("Unknown video type")
            VideoType.Dash ->
                DashMediaSource.Factory(getDataSourceFactory())
                    .createMediaSource(mediaItem)
            VideoType.Mp4,
            VideoType.Webm,
            ->
                ProgressiveMediaSource.Factory(getDataSourceFactory())
                    .createMediaSource(mediaItem)
            VideoType.Hls ->
                HlsMediaSource.Factory(getDataSourceFactory())
                    .createMediaSource(mediaItem)
        }

        // Prepare the player with the media source.
        player.setMediaSource(mediaSource)
        player.prepare()

        val videoState = config.videoState
        if (videoState != null) {
            player.seekTo(videoState.currentTime)
            if (config.isInline) {
                player.volume = videoState.volume ?: preferences.inlineVideoDefaultVolume
            }
        } else {
            if (config.isInline) {
                player.volume = preferences.inlineVideoDefaultVolume
            }
        }

        player.playWhenReady = config.playing
    }
}
