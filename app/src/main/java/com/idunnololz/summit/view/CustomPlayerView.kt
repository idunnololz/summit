package com.idunnololz.summit.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_GET_AUDIO_ATTRIBUTES
import androidx.media3.ui.PlayerView
import com.idunnololz.summit.R
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

    fun getVideoState(): VideoState? = player?.getVideoState()
}
