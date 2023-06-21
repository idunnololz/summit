package com.idunnololz.summit.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import com.idunnololz.summit.R
import com.idunnololz.summit.offline.OfflineManager
import io.reactivex.Single
import kotlinx.coroutines.runInterruptible
import okhttp3.Request
import okio.*
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer

object FileDownloadHelper {

    private val TAG = FileDownloadHelper::class.java.canonicalName

//    fun downloadSkin(
//        context: Context,
//        skinName: String?,
//        championNameEn: String,
//        url: String,
//        cacheFile: File? = null
//    ): Single<DownloadResult> {
//        val name = if (skinName == null || skinName == "default") {
//            championNameEn
//        } else {
//            skinName
//        }.replace(':', '_').replace(' ', '_')
//
//        val destFileName = "$name.jpg"
//
//        return downloadFile(context, destFileName, url, cacheFile)
//            .doAfterSuccess {
//                // log event
//                val bundle = Bundle()
//                bundle.putString("url", url)
//                bundle.putString("skin_dl_to_dir", it.toString())
//                FirebaseAnalytics.getInstance(context).logEvent("download_splash", bundle)
//            }
//    }
//
//    enum class Quality {
//        BEST,
//        WORST
//    }
//
//    /**
//     * Downloads one of the Dash video's video and audio stream, muxes them together and saves the
//     * result.
//     */
//    fun downloadDashVideo(
//        context: Context,
//        offlineManager: OfflineManager,
//        url: String,
//        quality: Quality,
//    ): Single<DownloadResult> = Single.create a@{ emitter ->
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            emitter.onError(RuntimeException("API level too low!"))
//            return@a
//        }
//
//        val destFileName = FilenameUtils.getName(url)
//
//        val uri = Uri.parse(url)
//        val cache = offlineManager.exoCache
//        val factory = DefaultHttpDataSourceFactory("ExoPlayer", null)
//        val constructorHelper =
//            DownloaderConstructorHelper(cache, factory)
//        val dataSpec = DataSpec(
//            uri,  /* absoluteStreamPosition= */
//            0,  /* length= */
//            C.LENGTH_UNSET.toLong(),  /* key= */
//            null,  /* flags= */
//            DataSpec.FLAG_ALLOW_GZIP
//        )
//        val dataSource = constructorHelper.createCacheDataSource()
//        // Create a downloader for the first variant in a master playlist.
//        val manifestDownloader = MyDashDownloader(
//            uri,
//            listOf(),
//            constructorHelper
//        )
//        val manifest = manifestDownloader.getManifest(dataSource, dataSpec)
//        val videoGroupIndex = manifest.getPeriod(0).adaptationSets.indexOfFirst { it.type == C.TRACK_TYPE_VIDEO }
//        val audioGroupIndex = manifest.getPeriod(0).adaptationSets.indexOfFirst { it.type == C.TRACK_TYPE_AUDIO }
//        val videoTrack = manifest.getPeriod(0).adaptationSets[videoGroupIndex]
//        var videoTrackIndex = 0
//
//        when (quality) {
//            Quality.BEST -> {
//                var highestBitrate = 0
//                videoTrack.representations.withIndex().forEach { (index, rep) ->
//                    if (rep.format.bitrate > highestBitrate) {
//                        highestBitrate = rep.format.bitrate
//                        videoTrackIndex = index
//                    }
//                }
//            }
//            Quality.WORST -> {
//                var lowestBitrate = 0
//                videoTrack.representations.withIndex().forEach { (index, rep) ->
//                    if (rep.format.bitrate < lowestBitrate) {
//                        lowestBitrate = rep.format.bitrate
//                        videoTrackIndex = index
//                    }
//                }
//            }
//        }
//
//        val streamKeys: List<StreamKey>
//        val hasAudio: Boolean
//        if (audioGroupIndex == -1) {
//            hasAudio = false
//            streamKeys = listOf(
//                StreamKey(videoGroupIndex, videoTrackIndex)
//            )
//        } else {
//            hasAudio = true
//            streamKeys = listOf(
//                StreamKey(videoGroupIndex, videoTrackIndex),
//                StreamKey(audioGroupIndex, 0)
//            )
//        }
//        // Create a downloader for the first variant in a master playlist.
//        val dashDownloader = MyDashDownloader(
//            uri,
//            streamKeys,
//            constructorHelper
//        )
//
//        Log.d(TAG, "Attempting to download DASH video...")
//        try {
//            dashDownloader.download { contentLength, bytesDownloaded, percentDownloaded ->
//                Log.d(TAG, "Downloading video $url. Progress: $percentDownloaded")
//            }
//        } catch (e: IndexOutOfBoundsException) {
//            Log.e(TAG, "No audio stream...", e)
//        }
//
//        Log.d(TAG, "Done downloading DASH segments...")
//
//        val dashManifest: DashManifest = dashDownloader.cachedManifest ?: run {
//            emitter.onError(RuntimeException("Error downloading/parsing DASH manifest."))
//            return@a
//        }
//
//        // Access downloaded data using CacheDataSource
//        val cacheDataSource =
//            CacheDataSource(cache, factory.createDataSource(), CacheDataSource.FLAG_BLOCK_ON_CACHE)
//        val videoSegments = dashDownloader.getMySegments(
//            cacheDataSource,
//            dashManifest.copy(listOf(StreamKey(videoGroupIndex, videoTrackIndex))),
//            false).sortedBy { it.dataSpec.absoluteStreamPosition }
//        val audioSegments: List<MyDashDownloader.MySegment>? = if (hasAudio) {
//            dashDownloader.getMySegments(
//                cacheDataSource,
//                dashManifest.copy(listOf(StreamKey(audioGroupIndex, 0))),
//                false).sortedBy { it.dataSpec.absoluteStreamPosition }
//        } else null
//
//        fun mergeSegmentsToFile(file: File, segments: List<MyDashDownloader.MySegment>) {
//            val buffer = ByteArray(4096)
//            file.createNewFile()
//            file.sink().buffer().use { bufferedSink ->
//
//                segments.forEach { segment ->
//                    cacheDataSource.open(segment.dataSpec)
//
//                    do {
//                        val read = cacheDataSource.read(buffer, 0, buffer.size)
//                        bufferedSink.write(buffer, 0, read)
//                    } while (read == buffer.size)
//                }
//            }
//        }
//
//        offlineManager.videoCacheDir.mkdirs()
//
//        val mergedVideoFile = File(offlineManager.videoCacheDir, "$destFileName.v")
//        mergedVideoFile.delete()
//        mergeSegmentsToFile(mergedVideoFile, videoSegments)
//
//        val destFile = File(offlineManager.videoCacheDir, destFileName)
//        destFile.delete()
//
//        if (hasAudio) {
//            val mergedAudioFile = File(offlineManager.videoCacheDir, "$destFileName.a")
//            mergedAudioFile.delete()
//            mergeSegmentsToFile(mergedAudioFile, checkNotNull(audioSegments))
//
//            destFile.createNewFile()
//            val outputFile = destFile.absolutePath
//
//            val videoExtractor = MediaExtractor()
//            videoExtractor.setDataSource(mergedVideoFile.path)
//
//            val audioExtractor = MediaExtractor()
//            audioExtractor.setDataSource(mergedAudioFile.path);
//
//            Log.d(TAG, "Video Extractor Track Count " + videoExtractor.trackCount)
//            Log.d(TAG, "Audio Extractor Track Count " + audioExtractor.trackCount)
//
//            val muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
//            } else {
//                emitter.onError(RuntimeException("API level too low!"))
//                return@a
//            }
//
//            videoExtractor.selectTrack(0);
//            val videoFormat = videoExtractor.getTrackFormat(0)
//            val videoTrack = muxer.addTrack(videoFormat)
//
//            audioExtractor.selectTrack(0)
//            val audioFormat = audioExtractor.getTrackFormat(0)
//            val audioTrack = muxer.addTrack(audioFormat)
//
//            Log.d(TAG, "Video Format $videoFormat")
//            Log.d(TAG, "Audio Format $audioFormat")
//
//            var sawEOS = false
//            var frameCount = 0
//            val offset = 100
//            val sampleSize = 2 * 1024 * 1024
//            val videoBuf = ByteBuffer.allocate(sampleSize)
//            val audioBuf = ByteBuffer.allocate(sampleSize)
//            val videoBufferInfo = MediaCodec.BufferInfo()
//            val audioBufferInfo = MediaCodec.BufferInfo()
//
//            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
//            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
//
//            muxer.start()
//
//            while (!sawEOS) {
//                videoBufferInfo.offset = offset
//                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset)
//
//                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
//                    sawEOS = true
//                    videoBufferInfo.size = 0
//
//                } else {
//                    videoBufferInfo.presentationTimeUs = videoExtractor.sampleTime
//                    videoBufferInfo.flags = videoExtractor.sampleFlags
//                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo)
//                    videoExtractor.advance()
//
//                    frameCount++
//                }
//            }
//
//            var sawEOS2 = false
//            var frameCount2 = 0
//            while (!sawEOS2) {
//                frameCount2++;
//
//                audioBufferInfo.offset = offset
//                audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset)
//
//                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
//                    sawEOS2 = true
//                    audioBufferInfo.size = 0
//                } else {
//                    audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
//                    audioBufferInfo.flags = audioExtractor.getSampleFlags();
//                    muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo);
//                    audioExtractor.advance();
//                }
//            }
//
//            muxer.stop()
//            muxer.release()
//        } else {
//            mergedVideoFile.renameTo(destFile)
//        }
//
//        val result = downloadFile(
//            context,
//            destFileName,
//            null,
//            destFile,
//            "video/mp4"
//        ).blockingGet()
//
//        emitter.onSuccess(result)
//    }
//
    suspend fun downloadFile(
        c: Context,
        destFileName: String,
        url: String?,
        cacheFile: File? = null,
        mimeType: String? = null
    ): Result<DownloadResult> {
        val context = c.applicationContext
        val mimeType = mimeType
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url))
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(cacheFile?.absolutePath))

        val uriOrFilePath: String
        val outputStream: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

        val error = runInterruptible a@{
            try {
                Log.d(TAG, "Writing to file")
                val sink: BufferedSink = outputStream.sink().buffer()
                if (cacheFile?.exists() == true) {
                    sink.writeAll(cacheFile.source())
                } else {
                    val request = Request.Builder()
                        .url(checkNotNull(url))
                        .header("User-Agent", "Chrome")
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

        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri.parse(uriOrFilePath).also { uri ->
                context.contentResolver.update(uri, ContentValues().apply {
                    put(MediaStore.DownloadColumns.IS_PENDING, 0)
                }, null, null)
            }
        } else {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = downloadManager.addCompletedDownload(
                destFileName,
                context.getString(R.string.save_success_format, uriOrFilePath),
                true,
                mimeType,
                uriOrFilePath,
                File(uriOrFilePath).length(),
                true)
            downloadManager.getUriForDownloadedFile(id)
        }

        return Result.success(DownloadResult(uri, uriOrFilePath, mimeType))
    }

    class DownloadException : java.lang.Exception()

    data class DownloadResult(
        val uri: Uri,
        val filePath: String?,
        val mimeType: String?
    )
}