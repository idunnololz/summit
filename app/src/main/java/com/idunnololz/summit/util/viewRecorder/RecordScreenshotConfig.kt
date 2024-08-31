package com.idunnololz.summit.util.viewRecorder

import android.os.Parcelable
import androidx.annotation.FloatRange
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecordScreenshotConfig(
    val recordingLengthMs: Long = 5_000L,
    val recordingType: RecordingType = RecordingType.Mp4,
    val maxFps: Double = 32.0,
    @FloatRange(0.2, 1.0)
    val qualityFactor: Double = 0.75,
    @FloatRange(0.2, 1.0)
    val resolutionFactor: Double = 1.0,
) : Parcelable

enum class RecordingType {
    Gif,
    Mp4,
    Webm,
}
