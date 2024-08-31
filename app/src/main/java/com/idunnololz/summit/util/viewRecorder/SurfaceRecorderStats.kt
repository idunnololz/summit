package com.idunnololz.summit.util.viewRecorder

data class SurfaceRecorderStats(
    /**
     * The rate that frames are able to be drawn at.
     */
    val effectiveFrameRate: Double,
    /**
     * How long it took to draw a single frame. Excludes [delay()].
     */
    val frameTimeMs: Double,
)
