package com.idunnololz.summit.hidePosts

import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.preferences.Preferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Singleton
class HiddenPostsManager @Inject constructor(
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val hiddenPostsDao: HiddenPostsDao,
    private val preferences: Preferences,
) {
    private val coroutineScope = coroutineScopeFactory.create()

    private val onHiddenPostsChange = mutableMapOf<String, InstanceFlows>()
    private val instanceToCache = mutableMapOf<String, MutableSet<PostId>>()

    val hiddenPostsLimit: Int
        get() = HIDDEN_POSTS_LIMIT

    fun getInstanceFlows(instance: String): InstanceFlows {
        onHiddenPostsChange[instance]?.let {
            return it
        }

        val instanceFlows = InstanceFlows()
        onHiddenPostsChange[instance] = instanceFlows
        return instanceFlows
    }

    fun hidePost(postId: PostId, instance: String, hide: Boolean = true) {
        coroutineScope.launch {
            if (hide) {
                val entry = HiddenPostEntry(
                    0,
                    System.currentTimeMillis(),
                    instance,
                    postId,
                )
                hiddenPostsDao.insertHiddenPostRespectingTableLimit(entry)
                getHiddenPostEntriesInternal(instance).add(postId)

                getInstanceFlows(instance).apply {
                    onHiddenPostsChangeFlow.emit(Unit)
                    onHidePostFlow.emit(entry)
                }
            } else {
                hiddenPostsDao.deleteByPost(instance, postId)
                getHiddenPostEntriesInternal(instance).remove(postId)

                getInstanceFlows(instance).onHiddenPostsChangeFlow.emit(Unit)
            }
        }
    }

    fun clearHiddenPosts() {
        coroutineScope.launch {
            hiddenPostsDao.clear()
            instanceToCache.clear()

            onHiddenPostsChange.forEach {
                it.value.onHiddenPostsChangeFlow.emit(Unit)
            }
        }
    }

    suspend fun getHiddenPostsCount() = hiddenPostsDao.count()

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

    class InstanceFlows(
        val onHiddenPostsChangeFlow: MutableSharedFlow<Unit> = MutableSharedFlow(),
        val onHidePostFlow: MutableSharedFlow<HiddenPostEntry> = MutableSharedFlow(),
    )
}
