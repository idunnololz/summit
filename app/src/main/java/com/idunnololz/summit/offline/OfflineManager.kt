package com.idunnololz.summit.offline

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import androidx.core.net.toUri
import coil3.imageLoader
import com.idunnololz.summit.R
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.ServerApiException
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.util.Client
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.LinkUtils.USER_AGENT
import com.idunnololz.summit.util.PreferenceUtils
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.assertMainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okio.BufferedSink
import okio.buffer
import okio.sink

@SuppressLint("UnsafeOptInUsageError")
@Singleton
class OfflineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val directoryHelper: DirectoryHelper,
    private val lemmyApiClient: LemmyApiClient,
) {

    companion object {
        private val TAG = OfflineManager::class.java.simpleName
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val downloadTasks = LinkedHashMap<String, DownloadTask>()
    private val jobMap = HashMap<String, MutableList<Job>>()

    private val imageSizeHints = HashMap<String, Size>()
    private val maxImageSizeHint = HashMap<String, Size>()

    private var offlineDownloadProgressListeners = ArrayList<OfflineDownloadProgressListener>()

    private val downloadInProgressDir = directoryHelper.downloadInProgressDir
    private val imagesDir = directoryHelper.imagesDir
    private val videosDir = directoryHelper.videosDir
    private val videoCacheDir = directoryHelper.videoCacheDir

    private data class DownloadTask(
        val url: String,
        val listeners: LinkedList<TaskListener> = LinkedList(),
        val errorListeners: LinkedList<TaskFailedListener> = LinkedList(),
    )

    class Registration(
        private val key: String,
        private val listener: TaskListener,
        private val errorListener: TaskFailedListener?,
    ) {
        fun cancel(offlineManager: OfflineManager) {
            offlineManager.cancelFetch(key, listener, errorListener)
        }
    }

    fun fetchImage(rootView: View, url: String?, listener: TaskListener) {
        fetchImageWithError(rootView, url, listener, null)
    }

    fun fetchImageWithError(
        rootView: View,
        url: String?,
        listener: TaskListener,
        errorListener: TaskFailedListener?,
    ) {
        Log.d(TAG, "fetchImageWithError(): $url")
        url ?: return
        val registrations: MutableList<Registration> = (
            rootView.getTag(
                R.id.offline_manager_registrations,
            ) as? MutableList<Registration>
            ) ?: arrayListOf<Registration>().also {
            rootView.setTag(R.id.offline_manager_registrations, it)
        }
        registrations.add(
            fetchImage(url, listener, errorListener),
        )
    }

    fun cancelFetch(
        url: String,
        listener: TaskListener,
        errorListener: TaskFailedListener? = null,
    ) {
        assertMainThread()

        val task = downloadTasks[url]

        if (task != null) {
            task.listeners.remove(listener)
            task.errorListeners.remove(errorListener)

            if (task.listeners.isEmpty() && task.errorListeners.isEmpty()) {
                jobMap[url]?.forEach {
                    it.cancel()
                }
                jobMap.remove(url)
                downloadTasks.remove(url)
            }
        }
    }

    fun cancelFetch(rootView: View) {
        (
            rootView.getTag(
                R.id.offline_manager_registrations,
            ) as? MutableList<Registration>
            )?.forEach {
            it.cancel(this)
        }
    }

    fun setImageSizeHint(url: String, w: Int, h: Int) {
        imageSizeHints.getOrPut(url) { Size() }.apply {
            width = w
            height = h
        }
    }

    fun hasImageSizeHint(url: String): Boolean = imageSizeHints.containsKey(url)

    fun getImageSizeHint(url: String, size: Size): Size = size.apply {
        val size = imageSizeHints[url]
        if (size != null) {
            width = size.width
            height = size.height
        } else {
            width = 0
            height = 0
        }
    }

    fun setMaxImageSizeHint(file: File, w: Int, h: Int) {
        maxImageSizeHint.getOrPut(file.toUri().toString()) { Size() }.apply {
            width = w
            height = h
        }
    }

    fun hasMaxImageSizeHint(file: File): Boolean = maxImageSizeHint.containsKey(
        file.toUri().toString(),
    )

    fun getMaxImageSizeHint(file: File, size: Size): Size =
        getMaxImageSizeHint(file.toUri().toString(), size)

    fun getMaxImageSizeHint(url: String, size: Size): Size = size.apply {
        val size = maxImageSizeHint[url]
        if (size != null) {
            width = size.width
            height = size.height
        } else {
            width = 0
            height = 0
        }
    }

    private fun getFilenameForUrl(url: String): String {
        val baseUrl = url.split("?")[0]
        val extension = if (baseUrl.lastIndexOf(".") != -1) {
            baseUrl.substring(baseUrl.lastIndexOf("."))
        } else {
            ""
        }
        return Utils.hashSha256(url) + extension
    }

    private fun fetchGeneric(
        url: String,
        destDir: File,
        force: Boolean = false,
        saveToFileFn: (File) -> Result<Unit>,
        listener: TaskListener,
        errorListener: TaskFailedListener?,
        onComplete: (File) -> Unit,
    ): Registration {
        assertMainThread()
        check(!destDir.exists() || destDir.isDirectory)

        val task = downloadTasks[url]

        // This task is already scheduled... abort
        if (task != null) {
            task.listeners += listener
            if (errorListener != null) {
                task.errorListeners += errorListener
            }
            return Registration(url, listener, errorListener)
        }

        downloadTasks[url] = DownloadTask(url).apply {
            listeners += listener
            if (errorListener != null) {
                errorListeners += errorListener
            }
        }

        val job = coroutineScope.launch(Dispatchers.Default) {
            val result = withContext(Dispatchers.IO) {
                downloadFileIfNeeded(
                    url = url,
                    destDir = destDir,
                    force = force,
                    saveToFileFn = saveToFileFn,
                )
            }

            val file = result.fold(
                onSuccess = { it },
                onFailure = { error ->
                    Log.e(TAG, "", error)

                    // Delete downloaded file if there is an error in case the file is corrupt due to
                    // network issue, etc.
                    val downloadedFile = File(destDir, getFilenameForUrl(url))
                    downloadedFile.delete()

                    withContext(Dispatchers.Main) {
                        downloadTasks.remove(url)?.let {
                            it.errorListeners.forEach {
                                launch(Dispatchers.Main) {
                                    it(error)
                                }
                            }
                        }
                    }
                    null
                },
            ) ?: return@launch

            withContext(Dispatchers.Default) {
                onComplete(file)
            }

            withContext(Dispatchers.Main) {
                downloadTasks.remove(url)?.listeners?.forEach {
                    launch(Dispatchers.Main) {
                        it(file)
                    }
                }
            }
        }

        jobMap.getOrPut(url) { arrayListOf() }.add(job)

        return Registration(url, listener, errorListener)
    }

    private fun downloadFileIfNeeded(
        url: String,
        destDir: File,
        force: Boolean,
        saveToFileFn: (File) -> Result<Unit>,
    ): Result<File> {
        val fileName = getFilenameForUrl(url)
        val downloadedFile = File(destDir, fileName)
        val downloadingFile = File(downloadInProgressDir, fileName)
        Log.d(TAG, "dl file: " + downloadedFile.absolutePath)

        if (!force && downloadedFile.exists()) {
            return Result.success(downloadedFile)
        }

        downloadingFile.parentFile?.mkdirs()

        val result = saveToFileFn(downloadingFile)

        if (result.isFailure) {
            return Result.failure(requireNotNull(result.exceptionOrNull()))
        }

        if (downloadedFile.exists()) {
            downloadedFile.delete()
        }

        downloadedFile.parentFile?.mkdirs()
        downloadingFile.renameTo(downloadedFile)

        return Result.success(downloadedFile)
    }

    fun fetchImage(
        url: String,
        listener: TaskListener,
        errorListener: TaskFailedListener? = null,
        force: Boolean = false,
    ): Registration = fetchGeneric(
        url = url,
        destDir = imagesDir,
        force = force,
        saveToFileFn = { destFile ->
            val req = try {
                Request.Builder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "*/*")
                    .url(url)
                    .build()
            } catch (e: Exception) {
                return@fetchGeneric Result.failure(e)
            }

            try {
                val response = Client.get().newCall(req).execute()

                if (response.code == 200) {
                    val sink: BufferedSink = destFile.sink().buffer()
                    response.body?.source()?.let {
                        sink.writeAll(it)
                    }
                    sink.close()

                    Log.d(TAG, "Downloaded image from $url")

                    Result.success(Unit)
                } else if (response.code >= 500) {
                    Result.failure(ServerApiException(response.code))
                } else {
                    Result.failure(
                        ClientApiException(
                            "${response.message} url: $url",
                            response.code,
                        ),
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        },
        listener = listener,
        errorListener = errorListener,
        onComplete = {
            calculateImageMaxSizeIfNeeded(it)
        },
    )

    fun calculateImageMaxSizeIfNeeded(file: File) {
        if (hasMaxImageSizeHint(file)) {
            return
        }

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        // Returns null, sizes are in the options variable
        BitmapFactory.decodeFile(file.path, options)
        val width = options.outWidth
        val height = options.outHeight

        if (width > Utils.getScreenWidth(context) ||
            height > Utils.getScreenHeight(context)
        ) {
            val size =
                LemmyUtils.calculateMaxImagePreviewSize(
                    context,
                    width,
                    height,
                )
            setMaxImageSizeHint(file, size.width, size.height)
        } else {
            setMaxImageSizeHint(file, -1, -1)
        }
    }

    fun doOfflineBlocking(config: OfflineTaskConfig) {
        // TODO()
    }

    fun getLastSuccessfulOfflineDownloadTime(): Long = PreferenceUtils.preferences.getLong(
        PreferenceUtils.KEY_LAST_SUCCESSFUL_OFFLINE_DOWNLOAD,
        -1,
    )

//    fun postProgressUpdate(message: String, progress: Double) {
//        val copy = offlineDownloadProgressListeners.toList()
//        AndroidSchedulers.mainThread().scheduleDirect {
//            for (l in copy) {
//                l(message, progress)
//            }
//        }
//    }

    fun addOfflineDownloadProgressListener(
        listener: OfflineDownloadProgressListener,
    ): OfflineDownloadProgressListener {
        offlineDownloadProgressListeners.add(listener)
        return listener
    }

    fun removeOfflineDownloadProgressListener(listener: OfflineDownloadProgressListener?) {
        offlineDownloadProgressListeners.remove(listener)
    }

    fun clearOfflineData() {
        Utils.deleteDir(imagesDir)
        Utils.deleteDir(videosDir)
        Utils.deleteDir(videoCacheDir)

        lemmyApiClient.clearCache()
        context.imageLoader.diskCache?.clear()

        imagesDir.mkdirs()
        videosDir.mkdirs()
        videoCacheDir.mkdirs()
    }
}
