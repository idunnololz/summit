package com.idunnololz.summit.hidePosts

import android.util.Log
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiddenPostsManager @Inject constructor(
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val hiddenPostsDao: HiddenPostsDao,
) {
    private val coroutineScope = coroutineScopeFactory.create()

    private val onHiddenPostsChange = mutableMapOf<String, MutableSharedFlow<Unit>>()
    private val instanceToCache = mutableMapOf<String, MutableSet<PostId>>()

    init {
        coroutineScope.launch {
            Log.d("dbdb", "hiddenPostsDao: ${hiddenPostsDao.count()}")
        }
    }

    fun getOnHiddenPostsChangeFlow(instance: String): MutableSharedFlow<Unit> {
        onHiddenPostsChange[instance]?.let {
            return it
        }

        val flow = MutableSharedFlow<Unit>()
        onHiddenPostsChange[instance] = flow
        return flow
    }

    fun hidePost(postId: PostId, instance: String) {
        coroutineScope.launch {
            val entry = HiddenPostEntry(
                0,
                System.currentTimeMillis(),
                instance,
                postId,
            )
            hiddenPostsDao.insertHiddenPostRespectingTableLimit(entry)
            getHiddenPostEntriesInternal(instance).add(postId)

            getOnHiddenPostsChangeFlow(instance).emit(Unit)
        }
    }

    fun clearHiddenPosts() {
        coroutineScope.launch {
            hiddenPostsDao.clear()
            instanceToCache.clear()

            onHiddenPostsChange.forEach {
                it.value.emit(Unit)
            }
        }
    }

    suspend fun getHiddenPostsCount() =
        hiddenPostsDao.count()

    suspend fun getHiddenPostEntries(instance: String): Set<PostId> =
        getHiddenPostEntriesInternal(instance)

    private suspend fun getHiddenPostEntriesInternal(instance: String): MutableSet<PostId> {
        instanceToCache[instance]?.let {
            return it
        }

        val hiddenPosts = mutableSetOf<PostId>()
        hiddenPostsDao.getHiddenPosts(instance).mapTo(hiddenPosts) { it.postId }
        instanceToCache[instance] = hiddenPosts
        return hiddenPosts
    }
}
