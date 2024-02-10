package com.idunnololz.summit.hidePosts

import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.preferences.Preferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiddenPostsManager @Inject constructor(
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val hiddenPostsDao: HiddenPostsDao,
    private val preferences: Preferences,
) {
    private val coroutineScope = coroutineScopeFactory.create()

    private val onHiddenPostsChange = mutableMapOf<String, MutableSharedFlow<Unit>>()
    private val instanceToCache = mutableMapOf<String, MutableSet<PostId>>()

    val hiddenPostsLimit: Int
        get() = HIDDEN_POSTS_LIMIT

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

    suspend fun getAllHiddenPostEntries(): List<HiddenPost> =
        hiddenPostsDao.getAllHiddenPosts().map {
            HiddenPost(
                it.id,
                it.postId,
                it.instance,
                it.ts,
            )
        }

    private suspend fun getHiddenPostEntriesInternal(instance: String): MutableSet<PostId> {
        if (!preferences.isHiddenPostsEnabled) {
            return mutableSetOf()
        }

        instanceToCache[instance]?.let {
            return it
        }

        val hiddenPosts = mutableSetOf<PostId>()
        hiddenPostsDao.getHiddenPosts(instance).mapTo(hiddenPosts) { it.postId }
        instanceToCache[instance] = hiddenPosts
        return hiddenPosts
    }

    suspend fun removeEntry(entryId: Long) {
        hiddenPostsDao.deleteByEntryId(entryId)
        instanceToCache.clear()
    }

    data class HiddenPost(
        val id: Long,
        val hiddenPostId: PostId,
        val instance: String,
        val ts: Long,
    )
}
