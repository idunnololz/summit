package com.idunnololz.summit.lemmy.userTags

import android.util.Log
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Singleton
class UserTagsManager @Inject constructor(
  private val dao: UserTagsDao,
  private val coroutineScopeFactory: CoroutineScopeFactory,
) {

  companion object {
    private const val TAG = "UserTagsManager"
  }

  private val coroutineScope = coroutineScopeFactory.createConfined()

  private var userTagsByName: Map<String, UserTag> = mapOf()
  private var recentTagsCache: List<UserTag>? = null

  val onChangedFlow = MutableSharedFlow<Unit>()

  suspend fun init() {
    initialize()
  }

  private suspend fun initialize() {
    reload()
  }

  private suspend fun reload() {
    Log.d(TAG, "reload()")

    userTagsByName = dao.getAllUserTags()
      .associate { it.actorId to it.toUserTag() }
  }

  fun addOrUpdateTag(personName: String, tag: String, fillColor: Int, strokeColor: Int) {
    if (personName.isBlank()) {
      return
    }

    if (tag.isBlank()) {
      return
    }

    coroutineScope.launch {
      val entry = dao.getUserTagByName(personName).firstOrNull()
      val ts = System.currentTimeMillis()

      if (entry == null) {
        dao.insertUserTag(
          UserTagEntry(
            id = 0,
            actorId = personName.lowercase(Locale.US),
            tag = UserTagConfig(
              tag,
              fillColor,
              strokeColor,
            ),
            createTs = ts,
            updateTs = ts,
            usedTs = ts,
          ),
        )
      } else {
        dao.insertUserTag(
          UserTagEntry(
            id = entry.id,
            actorId = personName.lowercase(Locale.US),
            tag = UserTagConfig(
              tag,
              fillColor,
              strokeColor,
            ),
            createTs = entry.createTs,
            updateTs = ts,
            usedTs = ts,
          ),
        )
      }

      onChanged()
    }
  }

  suspend fun getAllUserTagEntries(): List<UserTagEntry> {
    return coroutineScope.async {
      dao.getAllUserTags()
    }.await()
  }

  fun getUserTag(fullName: String): UserTagConfig? {
    return userTagsByName[fullName.lowercase(Locale.US)]?.config
  }

  fun getUserTags() = userTagsByName.values.toList()

  fun deleteTag(personName: String) {
    coroutineScope.launch {
      dao.getUserTagByName(personName).forEach {
        dao.delete(it)
      }

      onChanged()
    }
  }

  fun onTagUsed(tag: UserTag) {
    coroutineScope.launch {
      dao.getUserTagById(tag.id).forEach {
        dao.insertUserTag(
          it.copy(
            usedTs = System.currentTimeMillis(),
          ),
        )
      }

      onChanged()
    }
  }

  suspend fun getRecentTags(): List<UserTag> {
    val recentTagsCache = recentTagsCache
    if (recentTagsCache != null) {
      return recentTagsCache
    }

    val seenTags = mutableSetOf<UserTagConfig>()
    val recent = coroutineScope.async {
      dao.getRecentUserTags()
        .mapNotNull {
          if (seenTags.add(it.tag)) {
            it.toUserTag()
          } else {
            null
          }
        }
        .also {
          this@UserTagsManager.recentTagsCache = it
        }
    }.await()

    return recent
  }

  private suspend fun onChanged() {
    reload()
    recentTagsCache = null // clear the cache since it's probably out of date

    onChangedFlow.emit(Unit)
  }
}
