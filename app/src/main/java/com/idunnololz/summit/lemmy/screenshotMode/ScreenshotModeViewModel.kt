package com.idunnololz.summit.lemmy.screenshotMode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.fileprovider.FileProviderHelper
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class ScreenshotModeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val generatedImageUri = StatefulLiveData<UriResult>()

    val screenshotConfig = MutableLiveData<ScreenshotConfig>(ScreenshotConfig())

    fun generateImageToSave(view: View, name: String) {
        generateImage(view, name, UriResult.Reason.Save)
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
                ),
            )
        }
    }

    data class UriResult(
        val uri: Uri,
        val reason: Reason,
    ) {
        enum class Reason {
            Share,
            Save,
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
