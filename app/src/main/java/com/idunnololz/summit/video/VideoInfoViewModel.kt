package com.idunnololz.summit.video

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.TrackGroupArray
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.guessMimeType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class VideoInfoViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
) : ViewModel() {

  data class VideoInfoModel(
    val url: String,
    val tracks: List<Track>
  ) {
    data class Track(
      val name: String,
      val formats: List<Format>
    )
    data class Format(
      val id: String?,
      val label: String?,
      val language: String?,
      val selectionFlags: Int = 0,
      val roleFlags: Int = 0,
      val auxiliaryTrackType: Int = 0,

      // Bitrates
      val averageBitrate: Int,
      val peakBitrate: Int,
      val bitrate: Int,

      // Codecs and metadata
      val codecs: String?,

      // Container specific
      val containerMimeType: String?,

      // Sample specific
      val sampleMimeType: String?,
      val maxInputSize: Int,
      val maxNumReorderSamples: Int,
      val initializationData: List<ByteArray>,
      val subsampleOffsetUs: Long,
      val hasPrerollSamples: Boolean,

      // Video specific
      val width: Int,
      val height: Int,
      val frameRate: Float,
      val rotationDegrees: Int,
      val pixelWidthHeightRatio: Float,
      val projectionData: ByteArray?,
      val stereoMode: Int,
      val maxSubLayers: Int,

      // Audio specific
      val channelCount: Int,
      val sampleRate: Int,
      val pcmEncoding: Int,
      val encoderDelay: Int,
      val encoderPadding: Int,

      // Text specific
      val accessibilityChannel: Int,
      val cueReplacementBehavior: Int,

      // Image specific
      val tileCountHorizontal: Int,
      val tileCountVertical: Int,

      // Crypto
      val cryptoType: Int,
    )
  }


  private val executor = Executors.newSingleThreadExecutor()
  val model = StatefulLiveData<VideoInfoModel>()

  @OptIn(UnstableApi::class)
  fun loadVideoInfo(url: String, force: Boolean = false) {
    model.setIsLoading()

    val uri = Uri.parse(url)
    val mediaItem = MediaItem.fromUri(uri)
    val trackGroupsFuture = MetadataRetriever.retrieveMetadata(context, mediaItem)
    Futures.addCallback(
      trackGroupsFuture,
      object : FutureCallback<TrackGroupArray?> {
        override fun onSuccess(trackGroups: TrackGroupArray?) {
          val tracks = if (trackGroups != null) {
            (0 until trackGroups.length).map { trackGroups.get(it) }.map { trackGroup ->
              VideoInfoModel.Track(
                name = trackGroup.id,
                formats = (0 until trackGroup.length).map { trackGroup.getFormat(it) }.map { format ->
                  VideoInfoModel.Format(
                    id = format.id,
                    label = format.label,
                    language = format.language,
                    selectionFlags = format.selectionFlags,
                    roleFlags = format.roleFlags,
                    auxiliaryTrackType = format.auxiliaryTrackType,
                    averageBitrate = format.averageBitrate,
                    peakBitrate = format.peakBitrate,
                    bitrate = format.bitrate,
                    codecs = format.codecs,
                    containerMimeType = format.containerMimeType,
                    sampleMimeType = format.sampleMimeType,
                    maxInputSize = format.maxInputSize,
                    maxNumReorderSamples = format.maxNumReorderSamples,
                    initializationData = format.initializationData,
                    subsampleOffsetUs = format.subsampleOffsetUs,
                    hasPrerollSamples = format.hasPrerollSamples,
                    width = format.width,
                    height = format.height,
                    frameRate = format.frameRate,
                    rotationDegrees = format.rotationDegrees,
                    pixelWidthHeightRatio = format.pixelWidthHeightRatio,
                    projectionData = format.projectionData,
                    stereoMode = format.stereoMode,
                    maxSubLayers = format.maxSubLayers,
                    channelCount = format.channelCount,
                    sampleRate = format.sampleRate,
                    pcmEncoding = format.pcmEncoding,
                    encoderDelay = format.encoderDelay,
                    encoderPadding = format.encoderPadding,
                    accessibilityChannel = format.accessibilityChannel,
                    cueReplacementBehavior = format.cueReplacementBehavior,
                    tileCountHorizontal = format.tileCountHorizontal,
                    tileCountVertical = format.tileCountVertical,
                    cryptoType = format.cryptoType,
                  )
                }
              )
            }
          } else {
            listOf()
          }

          model.postValue(
            VideoInfoModel(
              url,
              tracks,
            )
          )
        }

        override fun onFailure(t: Throwable) {
          model.postError(t)
        }
      },
      executor,
    )
  }

  override fun onCleared() {
    super.onCleared()
    executor.shutdown()
  }
}