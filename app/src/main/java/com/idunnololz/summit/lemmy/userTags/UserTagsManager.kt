package com.idunnololz.summit.lemmy.userTags

import android.util.Log
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserTagsManager @Inject constructor(
    private val dao: UserTagsDao,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    companion object {
        private const val TAG = "UserTagsManager"
    }

    private val coroutineScope = coroutineScopeFactory.createConfined()

    private var userTagsByName: Map<String, UserTagConfig> = mapOf()

    val onChangedFlow = MutableSharedFlow<Unit>()

    init {
        coroutineScope.launch {
            delay(1000)

            initialize()
        }
    }

    private suspend fun initialize() {
        reload()
    }

    private suspend fun reload() {
        Log.d(TAG, "reload()")

        userTagsByName = dao.getAllUserTags()
            .associate { it.tag.tagName to it.tag }
    }

    fun addOrUpdateTag(personName: String, tag: String, fillColor: Int, strokeColor: Int) {
        if (personName.isBlank()) {
            return
        }

        if (tag.isBlank()) {
            return
        }

        coroutineScope.launch {
            val entry = dao.getUserTag(personName).firstOrNull()
            val ts = System.currentTimeMillis()

            if (entry == null) {
                dao.insertUserTag(
                    UserTagEntry(
                        id = 0,
                        actorId = personName,
                        tag = UserTagConfig(
                            tag,
                            fillColor,
                            strokeColor,
                        ),
                        createTs = ts,
                        updateTs = ts,
                    )
                )
            } else {
                dao.insertUserTag(
                    UserTagEntry(
                        id = entry.id,
                        actorId = personName,
                        tag = UserTagConfig(
                            tag,
                            fillColor,
                            strokeColor,
                        ),
                        createTs = entry.createTs,
                        updateTs = ts,
                    )
                )
            }

            onChanged()
        }
    }

    private suspend fun onChanged() {
        reload()

        onChangedFlow.emit(Unit)
    }

    suspend fun getAllUserTags(): List<UserTagConfig> {
        return coroutineScope.async {
            dao.getAllUserTags()
        }.await().map { it.tag }
    }

    fun getUserTag(fullName: String): UserTagConfig? {
        return userTagsByName[fullName]
    }
}