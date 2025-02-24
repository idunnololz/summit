package com.idunnololz.summit.video

import android.os.Parcelable
import androidx.media3.common.Player
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoState(
    val currentTime: Long,
    val volume: Float?,
    val playing: Boolean,
) : Parcelable

fun Player.getVideoState(includeVolume: Boolean = true): VideoState = VideoState(
    currentTime = currentPosition,
    volume = if (includeVolume) {
        volume
    } else {
        null
    },
    playing = isPlaying,
)
