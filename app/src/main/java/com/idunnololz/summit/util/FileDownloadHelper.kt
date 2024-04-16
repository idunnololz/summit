package com.idunnololz.summit.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import com.idunnololz.summit.R
import com.idunnololz.summit.preferences.Preferences
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runInterruptible
import okhttp3.Request
import okio.BufferedSink
import okio.buffer
import okio.sink
import okio.source

@Singleton
class FileDownloadHelper @Inject constructor(
    private val preferences: Preferences,
) {

    companion object {
        private const val TAG = "FileDownloadHelper"
    }

    suspend fun downloadFile(
        c: Context,
        destFileName: String,
        url: String?,
        cacheFile: File? = null,
        mimeType: String? = null,
    ): Result<DownloadResult> {
        val downloadDirectory = preferences.downloadDirectory
        val useCustomDownloadDirectory = downloadDirectory != null
        val context = c.applicationContext
        val mimeType = mimeType
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url))
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(cacheFile?.absolutePath))

        val uriOrFilePath: String
        val outputStream: OutputStream? = if (downloadDirectory != null) {
            val oldParentUri = Uri.parse(downloadDirectory)
            val id = DocumentsContract.getTreeDocumentId(oldParentUri)
            val parentFolderUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(oldParentUri, id)
            val outputUri = try {
                DocumentsContract.createDocument(
                    c.contentResolver,
                    parentFolderUri,
                    mimeType ?: "*/*",
                    destFileName,
                )
            } catch (e: Exception) {
                return Result.failure(CustomDownloadLocationException("Create document failed", e))
            } ?: return Result.failure(CustomDownloadLocationException("Create document failed."))

            uriOrFilePath = outputUri.toString()

            c.contentResolver.openOutputStream(outputUri)
                ?: return Result.failure(CustomDownloadLocationException("Failed to open uri."))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.TITLE, destFileName)
                put(MediaStore.Downloads.DISPLAY_NAME, destFileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            // Insert into the database
            val contentResolver = context.contentResolver

            uriOrFilePath = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.toString() ?: run {
                return Result.failure(RuntimeException("Unable to insert into content resolver"))
            }
            uriOrFilePath.let {
                contentResolver.openOutputStream(Uri.parse(uriOrFilePath))
            }
        } else {
            val dlDir = context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (dlDir == null || !dlDir.exists() && !dlDir.mkdirs()) {
                return Result.failure(RuntimeException("Unable to make directory: $dlDir"))
            }

            val tokens = destFileName.split('.', limit = 2)

            val destFile = Utils.getNonconflictingFile(dlDir, tokens[0], tokens[1])
            uriOrFilePath = destFile.absolutePath
            destFile.outputStream()
        }

        if (outputStream == null) {
            return Result.failure(RuntimeException("Output stream was null!"))
        }

        outputStream.use {
            val error = runInterruptible a@{
                try {
                    Log.d(TAG, "Writing to file")
                    val sink: BufferedSink = outputStream.sink().buffer()
                    if (cacheFile?.exists() == true) {
                        sink.writeAll(cacheFile.source())
                    } else {
                        val request = Request.Builder()
                            .url(checkNotNull(url))
                            .header("User-Agent", LinkUtils.USER_AGENT)
                            .build()

                        val response = Client.get().newCall(request).execute()
                        try {
                            val body = response.body
                            if (body != null) {
                                sink.writeAll(body.source())
                            } else {
                                return@a DownloadException()
                            }
                        } finally {
                            response.body?.close()
                        }
                    }
                    sink.close()

                    null
                } catch (e: Exception) {
                    return@a e
                }
            }

            val uri: Uri = if (useCustomDownloadDirectory) {
                Uri.parse(uriOrFilePath)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Uri.parse(uriOrFilePath).also { uri ->
                    context.contentResolver.update(
                        uri,
                        ContentValues().apply {
                            put(MediaStore.DownloadColumns.IS_PENDING, 0)
                        },
                        null,
                        null,
                    )
                }
            } else {
                val downloadManager = context.getSystemService(
                    Context.DOWNLOAD_SERVICE,
                ) as DownloadManager

                @Suppress("DEPRECATION")
                val id = downloadManager.addCompletedDownload(
                    destFileName,
                    context.getString(R.string.save_success_format, uriOrFilePath),
                    true,
                    mimeType,
                    uriOrFilePath,
                    File(uriOrFilePath).length(),
                    true,
                )
                downloadManager.getUriForDownloadedFile(id)
            }

            return Result.success(DownloadResult(uri, uriOrFilePath, mimeType))
        }
    }

    class DownloadException : Exception()

    class CustomDownloadLocationException : Exception {
        constructor(message: String?) : super(message)
        constructor(message: String?, cause: Throwable?) : super(message, cause)
    }

    data class DownloadResult(
        val uri: Uri,
        val filePath: String?,
        val mimeType: String?,
    )
}
