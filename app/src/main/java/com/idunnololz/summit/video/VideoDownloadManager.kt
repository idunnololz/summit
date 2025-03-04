package com.idunnololz.summit.video

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.UrlUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.lang.Exception
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private typealias DownloadId = String

@Singleton
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class VideoDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val directoryHelper: DirectoryHelper,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    companion object {
        private const val TAG = "VideoDownloadManager"
    }

    private val databaseProvider = StandaloneDatabaseProvider(context)

    private val downloadCache =
        SimpleCache(directoryHelper.videosDir, NoOpCacheEvictor(), databaseProvider)

    private val dataSourceFactory = DefaultHttpDataSource.Factory()
    private val downloadExecutor = Executor(Runnable::run)

    private val downloadRequestsContext = Dispatchers.IO.limitedParallelism(1)

    private val downloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            downloadExecutor,
        )

    private val downloadRequests = mutableMapOf<DownloadId, DownloadVideoRequest>()

    private val coroutineScope = coroutineScopeFactory.create()

    init {
        downloadManager.addListener(
            object : DownloadManager.Listener {
                override fun onDownloadChanged(
                    downloadManager: DownloadManager,
                    download: Download,
                    finalException: Exception?,
                ) {
                    super.onDownloadChanged(downloadManager, download, finalException)

                    Log.d(
                        TAG,
                        "onDownloadChanged(): ${download.request.uri} - " +
                            "s: ${download.state} ${download.percentDownloaded} - $finalException",
                    )

                    if (download.state == Download.STATE_COMPLETED) {
                        onDownloadComplete(download.request)
                    }
                }

                override fun onWaitingForRequirementsChanged(
                    downloadManager: DownloadManager,
                    waitingForRequirements: Boolean,
                ) {
                    super.onWaitingForRequirementsChanged(downloadManager, waitingForRequirements)
                }

                override fun onDownloadsPausedChanged(
                    downloadManager: DownloadManager,
                    downloadsPaused: Boolean,
                ) {
                    super.onDownloadsPausedChanged(downloadManager, downloadsPaused)
                }

                override fun onDownloadRemoved(
                    downloadManager: DownloadManager,
                    download: Download,
                ) {
                    super.onDownloadRemoved(downloadManager, download)
                }

                override fun onIdle(downloadManager: DownloadManager) {
                    super.onIdle(downloadManager)
                }

                override fun onRequirementsStateChanged(
                    downloadManager: DownloadManager,
                    requirements: Requirements,
                    notMetRequirements: Int,
                ) {
                    super.onRequirementsStateChanged(
                        downloadManager,
                        requirements,
                        notMetRequirements,
                    )
                }
            },
        )
        downloadManager.requirements = Requirements(Requirements.NETWORK)
        downloadManager.maxParallelDownloads = 3
    }

    private fun onDownloadComplete(exoDownloadRequest: DownloadRequest) {
        coroutineScope.launch(Dispatchers.Default) {
            val downloadRequest = withContext(downloadRequestsContext) {
                downloadRequests[exoDownloadRequest.id]
            } ?: return@launch

            val mediaItem = exoDownloadRequest.toMediaItem()
            val transformer = Transformer.Builder(context)
                .build()
            val outputFile = File(directoryHelper.videosDir, downloadRequest.finalFileName)

            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Result<File>> { continuation ->
                    transformer.addListener(
                        object : Transformer.Listener {
                            override fun onCompleted(
                                composition: Composition,
                                exportResult: ExportResult,
                            ) {
                                continuation.resume(Result.success(outputFile))
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException,
                            ) {
                                continuation.resume(Result.failure(exportException))
                            }
                        },
                    )
                    transformer.start(mediaItem, outputFile.absolutePath)
                }
            }

            downloadRequest.resultFlow.emit(Result.success(outputFile))
        }
    }

    suspend fun downloadVideo(url: String): Result<File> {
        val request = DownloadVideoRequest(
            url = url,
            finalFileName = UrlUtils.getFileName(url),
            id = url,
        )

        withContext(downloadRequestsContext) {
            downloadRequests[request.id] = request
        }

//        withContext(Dispatchers.IO) {
//            val downloadRequest = request.toDownloadRequest()
//            downloadManager.addDownload(downloadRequest)
//            downloadManager.resumeDownloads()
//        }

        // Transformer appears to know how to download items by itself...
        onDownloadComplete(request.toDownloadRequest())

        return request.resultFlow.first { it != null }
            ?: Result.failure(RuntimeException("Unknown error"))
    }

    data class DownloadVideoRequest(
        val url: String,
        val finalFileName: String,
        val id: DownloadId,
    ) {
        val resultFlow = MutableStateFlow<Result<File>?>(null)
    }

    private fun DownloadVideoRequest.toDownloadRequest() =
        DownloadRequest.Builder(id, Uri.parse(url)).build()
}
