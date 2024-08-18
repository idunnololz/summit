package com.idunnololz.summit.lemmy.utils.stateStorage

import android.content.SharedPreferences
import com.idunnololz.summit.preferences.StateSharedPreference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent storage for things that are meant to persist state of the app. DO NOT USE for
 * preferences. These things should not be carried over to another app if preferences are exported
 * for instance.
 */
class AccountStateStorage(
    private val accountId: Long,
    private val accountInstance: String,
    private val prefs: SharedPreferences,
) {

    companion object {
        private const val KEY_LAST_CONVERSATION_REFRESH_TS = "KEY_LAST_CONVERSATION_REFRESH_TS"
        private const val KEY_CONVERSATION_EARLIEST_MESSAGE_TS =
            "KEY_CONVERSATION_EARLIEST_MESSAGE_TS"
    }

    var lastConversationRefreshTs: Long
        get() = prefs.getLong(KEY_LAST_CONVERSATION_REFRESH_TS, 0)
        set(value) {
            prefs.edit().putLong(KEY_LAST_CONVERSATION_REFRESH_TS, value).apply()
        }

    var conversationEarliestMessageTs: Long?
        get() = prefs.getLong(KEY_CONVERSATION_EARLIEST_MESSAGE_TS, 0)
        set(value) {
            prefs.edit()
                .apply {
                    if (value == null) {
                        remove(KEY_CONVERSATION_EARLIEST_MESSAGE_TS)
                    } else {
                        putLong(KEY_CONVERSATION_EARLIEST_MESSAGE_TS, value)
                    }
                }.apply()
        }

}
