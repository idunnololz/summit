package com.idunnololz.summit.lemmy.screenshotMode

import android.R.attr.duration
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.fileprovider.FileProviderHelper
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.gif.AnimatedGifEncoder
import com.idunnololz.summit.util.viewRecorder.ViewRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import kotlin.math.max


@HiltViewModel
class ScreenshotModeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        private const val TAG = "ScreenshotModeViewModel"
    }

    val generatedImageUri = StatefulLiveData<UriResult>()

    val screenshotConfig = MutableLiveData<ScreenshotConfig>(ScreenshotConfig())

    fun generateImageToSave(view: View, name: String) {
        generateImage(view, name, UriResult.Reason.Save)
    }

    fun generateGifToSave(view: View, name: String) {
        generateGif2(view, name, UriResult.Reason.Save)
    }

    private fun generateGif2(view: View, name: String, reason: UriResult.Reason) {

        viewModelScope.launch(Dispatchers.Main) {
            val (outputFile, fileUri) = FileProviderHelper(context)
                .getTempFile("Summit_$name.mp4")

            val viewRecorder = ViewRecorder()
            viewRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            viewRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            viewRecorder.setVideoFrameRate(32) // 5fps
            viewRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            viewRecorder.setVideoSize(720, 1280)
            viewRecorder.setVideoEncodingBitRate(2000 * 1000)
            viewRecorder.setOutputFile(outputFile.path)
            viewRecorder.setOnErrorListener { mr, what, extra -> Log.d(TAG, "Error: $what") }

            viewRecorder.setRecordedView(view)
            try {
                viewRecorder.prepare()
                viewRecorder.start()
            } catch (e: IOException) {
                Log.e(TAG, "startRecord failed", e)
                return@launch
            }

            withContext(Dispatchers.Default) {
                delay(3000)
            }

            withContext(Dispatchers.Main) {
                try {
                    viewRecorder.stop()
                    viewRecorder.reset()
                    viewRecorder.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {

                val uri = FileProviderHelper(context)
                    .openTempFile("Summit_$name.gif") {
                        convertMp4ToGif(outputFile, it)
                    }

                generatedImageUri.postValue(
                    UriResult(
                        uri = uri,
                        reason = reason,
                        fileType = UriResult.FileType.Gif,
                    ),
                )
            }
        }
    }

    private suspend fun convertMp4ToGif(outputFile: File, outputStream: OutputStream) {
        val frameRate = 24f
        val gifEncoder = AnimatedGifEncoder()
        val os = BufferedOutputStream(outputStream)
        gifEncoder.setFrameRate(frameRate)
        gifEncoder.start(os)

        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(outputFile.path)
        val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.let { Integer.parseInt(it) }
            ?: return

        withContext(Dispatchers.Default) {
            val increment = (1000 / frameRate).toLong()
            val maxFrames = duration / increment
            Log.d(TAG, "Encoding ${maxFrames} frames...")

            var counter = 0L
            var i = 0
            while (counter < duration) {
                val b: Bitmap? = mmr.getFrameAtTime(counter * 1000, MediaMetadataRetriever.OPTION_CLOSEST)

                if (b == null) {
                    Log.d(TAG, "Bitmap was null!")
                }
                gifEncoder.addFrame(b)
                b?.recycle()

                counter += increment

                Log.d(TAG, "Encoded ${++i} / ${maxFrames} frames...")
            }
        }

        gifEncoder.finish()

        os.flush()
    }

    private fun generateImage(view: View, name: String, reason: UriResult.Reason) {
        generatedImageUri.setIsLoading()
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).apply {
                view.draw(this)
            }

            val fileUri = FileProviderHelper(context)
                .openTempFile("Summit_$name.png") {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }

            generatedImageUri.postValue(
                UriResult(
                    uri = fileUri,
                    reason = reason,
                    fileType = UriResult.FileType.Png,
                ),
            )
        }
    }

    private fun generateGif(view: View, name: String, reason: UriResult.Reason) {
        generatedImageUri.setIsLoading()

        viewModelScope.launch(Dispatchers.Default) {
            val gifEncoder = AnimatedGifEncoder()
            val output = ByteArrayOutputStream()
//            gifEncoder.init(view.width, view.height, file.path, GifEncoder.EncodingType.ENCODING_TYPE_NORMAL_LOW_MEMORY)

            gifEncoder.setFrameRate(24f)
            gifEncoder.start(output)

            val thread = Thread {
                val durationMs = 5_000
                var lastTimeMs = System.currentTimeMillis()
                var currentDurationMs = 0L

                val bitmaps = mutableListOf<Bitmap>()
                while (true) {
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    val s = System.currentTimeMillis()
                    view.draw(canvas)
                    Log.d(TAG, "drawtime: ${System.currentTimeMillis() - s}")

                    val now = System.currentTimeMillis()
                    val delay = now - lastTimeMs
                    lastTimeMs = now

                    currentDurationMs += delay

                    if (currentDurationMs > durationMs) {
                        break
                    }
                    Thread.sleep(max(1, 42L - delay)) // 24FPS
                    bitmaps.add(bitmap)
                }

                Log.d(TAG, "Encoding ${bitmaps.size} frames...")

                for ((index, b) in bitmaps.withIndex()) {
                    gifEncoder.addFrame(b)
                    b.recycle()

                    Log.d(TAG, "Encoding $index / ${bitmaps.size}")
                }

            }.apply {
                start()
            }

            thread.join()


            gifEncoder.finish()


            val uri = FileProviderHelper(context)
                .openTempFile("Summit_$name.gif") {
                    it.write(output.toByteArray())
                }

            generatedImageUri.postValue(
                UriResult(
                    uri = uri,
                    reason = reason,
                    fileType = UriResult.FileType.Gif,
                ),
            )
        }
    }

    data class UriResult(
        val uri: Uri,
        val reason: Reason,
        val fileType: FileType,
    ) {
        enum class Reason {
            Share,
            Save,
        }

        enum class FileType {
            Gif,
            Png,
            Mp4
        }
    }

    data class ScreenshotConfig(
        val postViewType: PostViewType = PostViewType.Full,
        val showPostDivider: Boolean = true,
    )

    enum class PostViewType {
        Full,
        ImageOnly,
        TextOnly,
        TitleOnly,
        TitleAndImageOnly,
        Compact,
    }
}

val ScreenshotModeViewModel.PostViewType.nextValue: ScreenshotModeViewModel.PostViewType
    get() {
        val enumValues = ScreenshotModeViewModel.PostViewType.values()
        val nextValue = enumValues[(ordinal + 1) % enumValues.size]

        return nextValue
    }
