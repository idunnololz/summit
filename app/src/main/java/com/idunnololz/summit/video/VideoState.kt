package com.idunnololz.summit.video

import android.os.Parcelable
import androidx.media3.common.Player
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoState(
    val currentTime: Long,
    val volume: Float,
) : Parcelable

fun Player.getVideoState(): VideoState =
    VideoState(
        currentTime = currentPosition,
        volume = volume,
    )
