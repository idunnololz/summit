package com.idunnololz.summit.lemmy.userTags

import android.util.Log
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.actions.PostReadManager.Companion
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.Locale
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

    suspend fun init() {
        initialize()
    }

    private suspend fun initialize() {
        reload()
    }

    private suspend fun reload() {
        Log.d(TAG, "reload()")

        userTagsByName = dao.getAllUserTags()
            .associate { it.actorId to it.tag }
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
                        actorId = personName.lowercase(Locale.US),
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
                        actorId = personName.lowercase(Locale.US),
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

    suspend fun getAllUserTags(): List<UserTagEntry> {
        return coroutineScope.async {
            dao.getAllUserTags()
        }.await()
    }

    fun getUserTag(fullName: String): UserTagConfig? {
        return userTagsByName[fullName.lowercase(Locale.US)]
    }

    fun deleteTag(personName: String) {
        coroutineScope.launch {
            dao.getUserTag(personName).forEach {
                dao.delete(it)
            }

            onChanged()
        }
    }

    private suspend fun onChanged() {
        reload()

        onChangedFlow.emit(Unit)
    }
}