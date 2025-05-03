package com.idunnololz.summit.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_GET_AUDIO_ATTRIBUTES
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.idunnololz.summit.R
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.video.VideoState
import com.idunnololz.summit.video.getVideoState

class CustomPlayerView : PlayerView {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr,
  )

  private var canRotate = false
  private var rotationLocked = false
  private var isVolumeChanged = false

  @OptIn(UnstableApi::class)
  fun setPlayerWithPreferences(player: Player?, preferences: Preferences) {
    val customPlayerListener =
      (getTag(R.id.custom_player_listener) as? Player.Listener) ?:
      object : Player.Listener {

        @OptIn(UnstableApi::class)
        override fun onIsPlayingChanged(isPlaying: Boolean) {
          if (isPlaying) {
            if (preferences.autoHideUiOnPlay) {
              hideController()
            }
          } else {
            // Not playing because playback is paused, ended, suppressed, or the player
            // is buffering, stopped or failed. Check player.playWhenReady,
            // player.playbackState, player.playbackSuppressionReason and
            // player.playerError for details.
          }
        }
      }.also {
        setTag(R.id.custom_player_listener, it)
      }

    this.player?.removeListener(customPlayerListener)

    if (player != null) {
      if (preferences.tapAnywhereToPlayPause) {
        setOnClickListener {
          if (!isControllerFullyVisible && player.isPlaying) {
            player.pause()
          }
        }
        findViewById<View>(R.id.controller_root_view).setOnClickListener {
          player.play()
        }
      }
    }

    this.setPlayer(player)

    player?.addListener(customPlayerListener)
  }

  override fun setPlayer(player: Player?) {
    super.setPlayer(player)

    player ?: return

    val muteButton = findViewById<ImageButton>(R.id.exo_volume_control)

    if (!player.isCommandAvailable(COMMAND_GET_AUDIO_ATTRIBUTES)) {
      muteButton.visibility = View.GONE
    } else {
      if (player.volume <= 0.1f) {
        muteButton.setImageResource(R.drawable.baseline_volume_off_24)
      } else {
        muteButton.setImageResource(R.drawable.baseline_volume_up_24)
      }
      muteButton.setOnClickListener {
        isVolumeChanged = true
        if (player.volume <= 0.1f) {
          player.volume = 1f
          muteButton.setImageResource(R.drawable.baseline_volume_up_24)
        } else {
          player.volume = 0f
          muteButton.setImageResource(R.drawable.baseline_volume_off_24)
        }
      }
    }
  }

  fun getRotateControl(): ImageButton = findViewById(R.id.exo_rotate_control)

  fun getVideoState(): VideoState? = player?.getVideoState(includeVolume = isVolumeChanged)

}
