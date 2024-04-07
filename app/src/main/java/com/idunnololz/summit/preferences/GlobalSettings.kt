package com.idunnololz.summit.preferences

/**
 * Settings that are stored in global state for easy access.
 */
object GlobalSettings {

    var warnReplyToOldContentThresholdMs: Long? = null
        private set

    fun refresh(preferences: Preferences) {
        warnReplyToOldContentThresholdMs = if (preferences.warnReplyToOldContent) {
            preferences.warnReplyToOldContentThresholdMs
        } else {
            null
        }
    }
}
