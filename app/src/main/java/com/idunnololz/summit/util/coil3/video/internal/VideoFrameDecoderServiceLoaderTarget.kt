package com.idunnololz.summit.util.coil3.video.internal

import coil3.annotation.InternalCoilApi
import coil3.util.DecoderServiceLoaderTarget
import coil3.video.VideoFrameDecoder

@OptIn(InternalCoilApi::class)
internal class VideoFrameDecoderServiceLoaderTarget : DecoderServiceLoaderTarget {
    override fun factory() = VideoFrameDecoder.Factory()
}
