package com.idunnololz.summit.util

import com.idunnololz.summit.video.VideoState

data class RecycledState(
    val videoState: VideoState?,
) {
    data class Builder(
        var videoState: VideoState? = null,
    ) {
        fun setVideoState(videoState: VideoState?) = apply {
            this.videoState = videoState
        }

        fun build() = RecycledState(videoState = videoState)
    }
}
