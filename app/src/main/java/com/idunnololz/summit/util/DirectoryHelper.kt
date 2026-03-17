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
  val miscCacheDir = File(context.cacheDir, "misc")
  val apiInfoCacheDir = File(context.cacheDir, "api_info")
  val listsCacheDir = File(context.cacheDir, "lists")

  val settingBackupsDir = File(context.filesDir, "sb")
  val saveForLaterDir = File(context.filesDir, "sfl")
  val accountDir = File(context.filesDir, "account")
  val downloadInProgressDir = File(context.filesDir, "dl")
  val imagesDir = File(context.filesDir, "imgs")
  val videosDir = File(context.filesDir, "videos")

  val listsDiskCache = JsonDiskCache
    .create(json, listsCacheDir, 1, 25L * 1024L * 1024L /* 25MB */)

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
}
