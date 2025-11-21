package com.idunnololz.summit.actions

import android.util.Log
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.db.PostReadDao
import com.idunnololz.summit.actions.db.ReadPostEntry
import com.idunnololz.summit.api.dto.lemmy.PostId
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.PostRef
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Singleton
class PostReadManager @Inject constructor(
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val accountManager: AccountManager,
  private val postReadDao: PostReadDao,
) {

  companion object {

    private const val TAG = "PostReadManager"

    const val MAX_READ_POST_LIMIT = 1000
  }

  class PostReadInfo(
    val read: Boolean,
    val ts: Long?,
  )

  private val coroutineScope = coroutineScopeFactory.create()

  val postReadChanged = MutableSharedFlow<Unit>()

  private val readPosts = object : LinkedHashMap<String, PostReadInfo>() {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PostReadInfo>?): Boolean =
      size > MAX_READ_POST_LIMIT
  }

  init {
    coroutineScope.launch {
      accountManager.currentAccountOnChange.collect {
        readPosts.clear()
        postReadChanged.emit(Unit)
        postReadDao.deleteAll()
      }
    }
  }

  suspend fun init() {
    val entries = postReadDao.getAll()
    for (entry in entries) {
      readPosts[entry.postKey] = PostReadInfo(
        read = entry.read,
        ts = entry.readTs,
      )
    }

    Log.d(TAG, "read posts: ${entries.size}")
  }

  fun isPostRead(instance: String, postId: PostId): Boolean? =
    readPosts[toKey(instance, postId)]?.read

  fun getPostReadInfo(instance: String, postId: PostId): PostReadInfo? =
    readPosts[toKey(instance, postId)]

  fun markPostAsReadLocal(instance: String, postId: PostId, read: Boolean) {
    val key = toKey(instance, postId)
    val now = System.currentTimeMillis()
    val oldReadInfo = readPosts[key]

    if (oldReadInfo?.read == read && (!read || (now - (oldReadInfo.ts ?: 0L) < 1000L))) {
      return
    }

    val readInfo = PostReadInfo(
      read = read,
      ts = now,
    )

    readPosts[key] = readInfo

    coroutineScope.launch {
      postReadChanged.emit(Unit)

      postReadDao.insert(ReadPostEntry(
        postKey = key,
        read = read,
        readTs = readInfo.ts
      ))
    }
  }

  fun delete(instance: String, id: Int) {
    val key = toKey(instance, id)
    readPosts.remove(key)

    coroutineScope.launch {
      postReadChanged.emit(Unit)

      postReadDao.delete(key)
    }
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun toKey(instance: String, postId: PostId): String = "$postId@$instance"
}
