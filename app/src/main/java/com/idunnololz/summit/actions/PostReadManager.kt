package com.idunnololz.summit.actions

import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.dto.PostId
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
) {

    companion object {
        private const val MAX_READ_POST_LIMIT = 1000
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
            }
        }
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
        }
    }

    fun delete(instance: String, id: Int) {
        readPosts.remove(toKey(instance, id))

        coroutineScope.launch {
            postReadChanged.emit(Unit)
        }
    }

    private fun PostRef.toKey(): String = toKey(instance, id)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun toKey(instance: String, postId: PostId): String = "$postId@$instance"
}
