package com.idunnololz.summit.lemmy.screenshotMode

import androidx.annotation.FloatRange

data class RecordScreenshotConfig(
    val recordingLengthMs: Long,
    val recordingType: RecordingType,
    val fps: Double,
    @FloatRange(0.0, 1.0)
    val qualityFactor: Double,
)

enum class RecordingType {
    Gif,
    Mp4
}