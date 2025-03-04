package com.idunnololz.summit.util

import android.content.Context
import android.util.Log
import com.idunnololz.summit.cache.JsonDiskCache
import com.idunnololz.summit.fileprovider.FileProviderHelper
import com.idunnololz.summit.lemmy.community.LoadedPostsData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class DirectoryHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    companion object {
        private const val TAG = "DirectoryHelper"
    }

    val cacheDir = context.cacheDir
    val okHttpCacheDir = File(context.cacheDir, "okhttp_cache")
    val videoCacheDir = File(context.cacheDir, "videos")
    val tabsCacheDir = File(context.cacheDir, "tabs")
    val miscCacheDir = File(context.cacheDir, "misc")

    val settingBackupsDir = File(context.filesDir, "sb")
    val saveForLaterDir = File(context.filesDir, "sfl")
    val accountDir = File(context.filesDir, "account")
    val downloadInProgressDir = File(context.filesDir, "dl")
    val imagesDir = File(context.filesDir, "imgs")
    val videosDir = File(context.filesDir, "videos")

    val tabsDiskCache = JsonDiskCache
        .create(json, tabsCacheDir, 1, 10L * 1024L * 1024L /* 10MB */)

    fun cleanup() {
        var purgedFiles = 0
        val thresholdTime = System.currentTimeMillis() - Duration.ofDays(1).toMillis()

        fun File.cleanupDir() {
            this.listFiles()?.forEach {
                if (it.lastModified() < thresholdTime) {
                    if (it.isDirectory) {
                        it.deleteRecursively()
                    } else {
                        it.delete()
                    }
                    purgedFiles++
                }
            }
        }

        imagesDir.cleanupDir()
        videosDir.cleanupDir()
        FileProviderHelper(context).fileProviderDir.cleanupDir()
        miscCacheDir.cleanupDir()

        Log.d(TAG, "Deleted $purgedFiles files.")
    }

    fun deleteOfflineImages() {
        imagesDir.deleteRecursively()
    }

    @Serializable
    data class PostListEngineCacheInfo(
        val totalPages: Int,
    )

    fun addPage(key: String?, secondaryKey: String?, data: LoadedPostsData, totalPages: Int) {
        key ?: return

        tabsCacheDir.mkdirs()

        val keyPrefix = "$key|$secondaryKey"
        val infoKey = "$keyPrefix|info"

        tabsDiskCache.cacheObject(infoKey, PostListEngineCacheInfo(totalPages))

        val pageKey = "$keyPrefix|${data.pageIndex}"

        tabsDiskCache.cacheObject(pageKey, data)

        tabsDiskCache.printDebugInfo()
    }

    fun clearPages(key: String?, secondaryKey: String?) {
        key ?: return

        tabsCacheDir.mkdirs()

        val keyPrefix = "$key|$secondaryKey"
        val infoKey = "$keyPrefix|info"

        tabsDiskCache.evict(infoKey)
    }

    fun getPages(key: String?, secondaryKey: String?): List<LoadedPostsData>? {
        key ?: return null

        tabsCacheDir.mkdirs()

        val keyPrefix = "$key|$secondaryKey"
        val infoKey = "$keyPrefix|info"

        val pageCacheInfo = tabsDiskCache.getCachedObject<PostListEngineCacheInfo>(infoKey)

        if (pageCacheInfo == null || pageCacheInfo.totalPages == 0) {
            return null
        }

        val pages = mutableListOf<LoadedPostsData>()
        for (i in 0 until pageCacheInfo.totalPages) {
            val pageKey = "$keyPrefix|$i"
            val postData = tabsDiskCache.getCachedObject<LoadedPostsData>(pageKey)
                ?: return null
            pages.add(postData)
        }

        return pages
    }
}
