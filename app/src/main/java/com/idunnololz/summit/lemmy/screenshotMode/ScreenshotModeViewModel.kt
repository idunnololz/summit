package com.idunnololz.summit.lemmy.screenshotMode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import com.idunnololz.summit.fileprovider.FileProviderHelper
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.viewRecorder.RecordScreenshotConfig
import com.idunnololz.summit.util.viewRecorder.RecordingType
import com.idunnololz.summit.util.viewRecorder.ViewRecorderHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class ScreenshotModeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val viewRecorderHelper: ViewRecorderHelper,
) : ViewModel() {

    companion object {
        private const val TAG = "ScreenshotModeViewModel"
    }

    var mimeType: String? = null
    var result: Uri? = null

    val generatedImageUri = StatefulLiveData<UriResult>()

    val screenshotConfig = MutableLiveData(ScreenshotConfig())

    val recordScreenshotConfig = MutableLiveData(RecordScreenshotConfig())

    var lastRecordingStats: ViewRecorderHelper.RecordingStats? = null

    fun generateImageToSave(view: View, name: String) {
        generateImage(view, name, UriResult.Reason.Save)
    }

    fun generateImageToShare(view: View, name: String) {
        generateImage(view, name, UriResult.Reason.Share)
    }

    fun recordScreenshot(
        view: View,
        name: String,
        reason: UriResult.Reason,
        config: RecordScreenshotConfig,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            viewRecorderHelper.recordView(
                view = view,
                name = name,
                config = config,
            ).collect {
                when (it) {
                    is ViewRecorderHelper.ViewRecordingState.Complete -> {
                        lastRecordingStats = it.recordingStats
                        generatedImageUri.postValue(
                            UriResult(
                                uri = it.fileUri,
                                reason = reason,
                                fileType = when (config.recordingType) {
                                    RecordingType.Gif -> UriResult.FileType.Gif
                                    RecordingType.Mp4 -> UriResult.FileType.Mp4
                                    RecordingType.Webm -> UriResult.FileType.Webm
                                },
                            ),
                        )
                    }
                    is ViewRecorderHelper.ViewRecordingState.Encoding -> {
                        generatedImageUri.postIsLoading(
                            statusDesc = ContextCompat.getContextForLanguage(context)
                                .getString(R.string.encoding),
                            progress = (it.progress * 100).toInt(),
                            maxProgress = 100,
                            isIndeterminate = false,
                        )
                    }
                    is ViewRecorderHelper.ViewRecordingState.Error -> {
                        generatedImageUri.postError(it.error)
                    }
                    is ViewRecorderHelper.ViewRecordingState.RecordingState -> {
                        generatedImageUri.postIsLoading(
                            statusDesc = ContextCompat.getContextForLanguage(context)
                                .getString(R.string.recording),
                            progress = (it.progress * 100).toInt(),
                            maxProgress = 100,
                            isIndeterminate = false,
                        )
                    }
                }
            }
        }
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
            Mp4,
            Webm,
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
        val enumValues = ScreenshotModeViewModel.PostViewType.entries.toTypedArray()
        val nextValue = enumValues[(ordinal + 1) % enumValues.size]

        return nextValue
    }
