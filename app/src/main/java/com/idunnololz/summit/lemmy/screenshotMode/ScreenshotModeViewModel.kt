package com.idunnololz.summit.lemmy.screenshotMode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.fileprovider.FileProviderHelper
import com.idunnololz.summit.util.gif.AnimatedGifEncoder
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
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
        generateGif(view, name, UriResult.Reason.Save)
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

        viewModelScope.launch {
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
