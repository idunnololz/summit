package com.idunnololz.summit.actions

import android.util.Log
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.db.PostReadDao
import com.idunnololz.summit.actions.db.ReadPostEntry
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.duplicatePostsDetector.DuplicatePostsDetector
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

    private val coroutineScope = coroutineScopeFactory.create()

    val postReadChanged = MutableSharedFlow<Unit>()

    private val readPosts = object : LinkedHashMap<String, Boolean>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
            return size > MAX_READ_POST_LIMIT
        }
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
            readPosts[entry.postKey] = entry.read
        }

        Log.d(TAG, "read posts: ${entries.size}")
    }

    fun isPostRead(instance: String, postId: PostId): Boolean? {
        return readPosts[toKey(instance, postId)]
    }

    fun markPostAsReadLocal(instance: String, postId: PostId, read: Boolean) {
        val key = toKey(instance, postId)
        if (readPosts[key] == read) {
            return
        }

        readPosts[key] = read

        coroutineScope.launch {
            postReadChanged.emit(Unit)

            postReadDao.insert(ReadPostEntry(key, read))
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

    private fun PostRef.toKey(): String = toKey(instance, id)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun toKey(instance: String, postId: PostId): String = "$postId@$instance"
}
